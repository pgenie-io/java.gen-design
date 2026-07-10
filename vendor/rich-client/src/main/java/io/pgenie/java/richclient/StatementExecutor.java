package io.pgenie.java.richclient;

import io.codemine.java.postgresql.jdbc.Statement;
import io.codemine.java.postgresql.jdbc.StatementBatch;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;

/**
 * Shared statement executor that wraps {@link Statement#executeOn(Connection)} and
 * {@link StatementBatch#execute(Connection)} with OpenTelemetry spans, a duration histogram
 * and slow-query logging.
 *
 * <p>All statement execution in {@code rich-client} flows through this class so that trace,
 * metric and log instrumentation is kept in one place.</p>
 */
public final class StatementExecutor {

    private static final String DB_SYSTEM = "postgresql";

    private static final String METRIC_NAME = "db.client.operation.duration";
    private static final String METRIC_UNIT = "s";
    private static final String METRIC_DESCRIPTION = "Duration of database client operations";

    private static final AttributeKey<String> DB_SYSTEM_NAME_KEY = AttributeKey.stringKey("db.system.name");
    private static final AttributeKey<String> DB_QUERY_TEXT_KEY = AttributeKey.stringKey("db.query.text");
    private static final AttributeKey<String> DB_OPERATION_NAME_KEY = AttributeKey.stringKey("db.operation.name");
    private static final AttributeKey<String> DB_COLLECTION_NAME_KEY = AttributeKey.stringKey("db.collection.name");
    private static final AttributeKey<String> STATEMENT_NAME_KEY = AttributeKey.stringKey("pgenie.statement.name");
    private static final AttributeKey<String> DB_USER_KEY = AttributeKey.stringKey("pgenie.db.user");
    private static final AttributeKey<Long> BATCH_SIZE_KEY = AttributeKey.longKey("db.operation.batch.size");

    private final Tracer tracer;
    private final DoubleHistogram durationHistogram;
    private final String dbUser;
    private final Duration slowQueryLogThreshold;
    private final Logger logger;

    /**
     * Creates a new executor.
     *
     * @param tracer               the OpenTelemetry tracer used to create CLIENT spans
     * @param meter                the OpenTelemetry meter used to derive the duration histogram
     * @param logger               the SLF4J logger used for slow-query warnings
     * @param dbUser               the database user to attach as {@code pgenie.db.user}
     * @param slowQueryLogThreshold queries running longer than this threshold are logged as slow;
     *                             zero logs every query; must not be negative
     * @throws NullPointerException if any argument is null
     */
    public StatementExecutor(
            Tracer tracer,
            Meter meter,
            Logger logger,
            String dbUser,
            Duration slowQueryLogThreshold) {
        this.tracer = Objects.requireNonNull(tracer, "tracer");
        Objects.requireNonNull(meter, "meter");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.dbUser = Objects.requireNonNull(dbUser, "dbUser");
        this.slowQueryLogThreshold = Objects.requireNonNull(slowQueryLogThreshold, "slowQueryLogThreshold");
        this.durationHistogram = meter.histogramBuilder(METRIC_NAME)
                .setUnit(METRIC_UNIT)
                .setDescription(METRIC_DESCRIPTION)
                .build();
    }

    /**
     * Execute a single statement on the supplied connection.
     *
     * @param statement  the statement to execute
     * @param connection the JDBC connection to use
     * @param parentSpan the parent span, or {@code null} for a root CLIENT span
     * @return the decoded statement result
     * @throws SQLException if a database access error occurs
     */
    public <R> R execute(Statement<R> statement, Connection connection, Span parentSpan) throws SQLException {
        Objects.requireNonNull(statement, "statement");
        Objects.requireNonNull(connection, "connection");
        String statementName = statement.getClass().getSimpleName();
        StatementMetadata metadata = metadataOf(statement);

        Span span = startStatementSpan(statementName, statement.sql(), metadata, parentSpan);
        long startNanos = System.nanoTime();
        try (var scope = span.makeCurrent()) {
            R result = statement.executeOn(connection);
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
            throw t;
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            double durationSeconds = durationNanos / 1_000_000_000.0;
            recordDuration(statement.sql(), statementName, metadata, durationSeconds);
            maybeLogSlowQuery(statementName, durationNanos, durationSeconds);
            span.end();
        }
    }

    /**
     * Execute a batch of statements on the supplied connection.
     *
     * <p>All statements must share the same SQL text and must not return rows.
     * A single CLIENT span and metric point is emitted for the whole batch. The metric
     * deliberately omits {@code db.operation.batch.size} to keep the cardinality shape
     * identical to the single-statement metric.</p>
     *
     * @param statements the statements to execute
     * @param connection the JDBC connection to use
     * @param parentSpan the parent span, or {@code null} for a root CLIENT span
     * @return the decoded results, in the same order as the input statements
     * @throws SQLException if a database access error occurs
     */
    public <R> List<R> executeBatch(
            Iterable<? extends Statement<R>> statements,
            Connection connection,
            Span parentSpan) throws SQLException {
        Objects.requireNonNull(statements, "statements");
        Objects.requireNonNull(connection, "connection");

        List<Statement<R>> statementList = copyStatements(statements);
        if (statementList.isEmpty()) {
            return List.of();
        }

        StatementBatch<R> batch = new StatementBatch<>(statementList);
        StatementMetadata metadata = metadataOf(statementList.get(0));
        String statementName = "batch";

        Span span = startBatchSpan(statementName, batch.sql(), batch.size(), metadata, parentSpan);
        long startNanos = System.nanoTime();
        try (var scope = span.makeCurrent()) {
            List<R> results = batch.execute(connection);
            span.setStatus(StatusCode.OK);
            return results;
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
            throw t;
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            double durationSeconds = durationNanos / 1_000_000_000.0;
            recordDuration(batch.sql(), statementName, metadata, durationSeconds);
            maybeLogSlowQuery(statementName, durationNanos, durationSeconds);
            span.end();
        }
    }

    private Span startStatementSpan(
            String statementName,
            String sql,
            StatementMetadata metadata,
            Span parentSpan) {
        var builder = tracer.spanBuilder(statementName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(DB_SYSTEM_NAME_KEY, DB_SYSTEM)
                .setAttribute(DB_QUERY_TEXT_KEY, sql)
                .setAttribute(STATEMENT_NAME_KEY, statementName)
                .setAttribute(DB_USER_KEY, dbUser);
        applyMetadataAttributes(builder, metadata);
        applyParentSpan(builder, parentSpan);
        return builder.startSpan();
    }

    private Span startBatchSpan(
            String statementName,
            String sql,
            int batchSize,
            StatementMetadata metadata,
            Span parentSpan) {
        var builder = tracer.spanBuilder(statementName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(DB_SYSTEM_NAME_KEY, DB_SYSTEM)
                .setAttribute(DB_QUERY_TEXT_KEY, sql)
                .setAttribute(STATEMENT_NAME_KEY, statementName)
                .setAttribute(BATCH_SIZE_KEY, (long) batchSize)
                .setAttribute(DB_USER_KEY, dbUser);
        applyMetadataAttributes(builder, metadata);
        applyParentSpan(builder, parentSpan);
        return builder.startSpan();
    }

    private void applyMetadataAttributes(io.opentelemetry.api.trace.SpanBuilder builder, StatementMetadata metadata) {
        if (metadata != null) {
            builder.setAttribute(DB_OPERATION_NAME_KEY, metadata.operationName());
            builder.setAttribute(DB_COLLECTION_NAME_KEY, metadata.collectionName());
        }
    }

    private void applyParentSpan(io.opentelemetry.api.trace.SpanBuilder builder, Span parentSpan) {
        if (parentSpan != null) {
            builder.setParent(io.opentelemetry.context.Context.current().with(parentSpan));
        }
    }

    private void recordDuration(
            String sql,
            String statementName,
            StatementMetadata metadata,
            double durationSeconds) {
        var attributesBuilder = Attributes.builder()
                .put(DB_SYSTEM_NAME_KEY, DB_SYSTEM)
                .put(DB_QUERY_TEXT_KEY, sql)
                .put(STATEMENT_NAME_KEY, statementName);
        if (metadata != null) {
            attributesBuilder.put(DB_OPERATION_NAME_KEY, metadata.operationName());
            attributesBuilder.put(DB_COLLECTION_NAME_KEY, metadata.collectionName());
        }
        durationHistogram.record(durationSeconds, attributesBuilder.build());
    }

    private void maybeLogSlowQuery(String statementName, long durationNanos, double durationSeconds) {
        if (Duration.ofNanos(durationNanos).compareTo(slowQueryLogThreshold) > 0) {
            logger.warn("Slow query detected: {} took {} seconds", statementName, durationSeconds);
        }
    }

    private static <R> List<Statement<R>> copyStatements(Iterable<? extends Statement<R>> statements) {
        List<Statement<R>> list = new ArrayList<>();
        statements.forEach(list::add);
        return list;
    }

    private static StatementMetadata metadataOf(Statement<?> statement) {
        return statement instanceof StatementMetadata metadata ? metadata : null;
    }
}
