package io.pgenie.java.richclient.observability;

import io.codemine.java.postgresql.jdbc.Statement;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.slf4j.Logger;

/**
 * Shared observability wrapper for statement and batch execution.
 *
 * <p>Owns the OpenTelemetry CLIENT spans, the {@code db.client.operation.duration}
 * histogram and SLF4J slow-query logging for all statement execution in {@code rich-client}.
 * The class is intentionally execution-agnostic: callers supply the database operation as a
 * {@link SqlOperation} and this class decorates it with telemetry.</p>
 */
public final class StatementObservability {

    static final String DB_SYSTEM = "postgresql";

    static final String METRIC_NAME = "db.client.operation.duration";
    static final String METRIC_UNIT = "s";
    static final String METRIC_DESCRIPTION = "Duration of database client operations";

    static final AttributeKey<String> DB_SYSTEM_NAME_KEY = AttributeKey.stringKey("db.system.name");
    static final AttributeKey<String> DB_QUERY_TEXT_KEY = AttributeKey.stringKey("db.query.text");
    static final AttributeKey<String> DB_OPERATION_NAME_KEY = AttributeKey.stringKey("db.operation.name");
    static final AttributeKey<String> DB_COLLECTION_NAME_KEY = AttributeKey.stringKey("db.collection.name");
    static final AttributeKey<String> STATEMENT_NAME_KEY = AttributeKey.stringKey("pgenie.statement.name");
    static final AttributeKey<String> DB_USER_KEY = AttributeKey.stringKey("pgenie.db.user");
    static final AttributeKey<Long> BATCH_SIZE_KEY = AttributeKey.longKey("db.operation.batch.size");

    private final Tracer tracer;
    private final DoubleHistogram durationHistogram;
    private final String dbUser;
    private final Duration slowQueryLogThreshold;
    private final Logger logger;

    /**
     * Creates a new statement observability wrapper.
     *
     * @param tracer               the OpenTelemetry tracer used to create CLIENT spans
     * @param meter                the OpenTelemetry meter used to derive the duration histogram
     * @param logger               the SLF4J logger used for slow-query warnings
     * @param dbUser               the database user to attach as {@code pgenie.db.user}
     * @param slowQueryLogThreshold queries running longer than this threshold are logged as slow;
     *                             zero logs every query; must not be negative
     * @throws NullPointerException if any argument is null
     */
    public StatementObservability(
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
     * Observes execution of a single statement.
     *
     * <p>Creates a CLIENT span named {@code statement.name()}, records the operation on the
     * {@code db.client.operation.duration} histogram and emits a slow-query warning if the
     * threshold is exceeded. The span is parented to {@code parentSpan} when non-null, otherwise
     * to the current OpenTelemetry context.</p>
     *
     * @param statement  the statement whose metadata and SQL define span/metric attributes
     * @param parentSpan the parent span, or {@code null} to use the current context
     * @param operation  the operation that actually executes the statement
     * @return the operation result
     * @throws SQLException if the operation throws
     */
    public <R> R observeStatement(
            Statement<?> statement,
            Span parentSpan,
            SqlOperation<R> operation) throws SQLException {
        Objects.requireNonNull(statement, "statement");
        Objects.requireNonNull(operation, "operation");

        String statementName = statement.statementName();
        Span span = startStatementSpan(statementName, statement.sql(), statement, parentSpan);
        return observe(span, statementName, statement.sql(), statement, operation);
    }

    /**
     * Observes execution of a batch of statements.
     *
     * <p>Creates a CLIENT span named {@code "batch"}, adds {@code db.operation.batch.size} to
     * the span, records the operation on the {@code db.client.operation.duration} histogram
     * without the batch-size attribute, and emits a slow-query warning if the threshold is
     * exceeded.</p>
     *
     * @param batchSql               the SQL text shared by every statement in the batch
     * @param representativeStatement a representative statement used for optional metadata
     *                               attributes ({@code db.operation.name}, {@code db.collection.name})
     * @param batchSize              the number of statements in the batch
     * @param parentSpan             the parent span, or {@code null} to use the current context
     * @param operation              the operation that actually executes the batch
     * @return the operation result
     * @throws SQLException if the operation throws
     */
    public <R> List<R> observeBatch(
            String batchSql,
            Statement<?> representativeStatement,
            int batchSize,
            Span parentSpan,
            SqlOperation<List<R>> operation) throws SQLException {
        Objects.requireNonNull(batchSql, "batchSql");
        Objects.requireNonNull(representativeStatement, "representativeStatement");
        Objects.requireNonNull(operation, "operation");

        String statementName = "batch";
        Span span = startBatchSpan(statementName, batchSql, batchSize, representativeStatement, parentSpan);
        return observe(span, statementName, batchSql, representativeStatement, operation);
    }

    private <R> R observe(
            Span span,
            String statementName,
            String sql,
            Statement<?> statement,
            SqlOperation<R> operation) throws SQLException {
        long startNanos = System.nanoTime();
        try (var scope = span.makeCurrent()) {
            R result = operation.execute();
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
            throw t;
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            double durationSeconds = durationNanos / 1_000_000_000.0;
            recordDuration(sql, statementName, statement, durationSeconds);
            maybeLogSlowQuery(statementName, durationNanos, durationSeconds);
            span.end();
        }
    }

    private Span startStatementSpan(
            String statementName,
            String sql,
            Statement<?> statement,
            Span parentSpan) {
        var builder = tracer.spanBuilder(statementName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(DB_SYSTEM_NAME_KEY, DB_SYSTEM)
                .setAttribute(DB_QUERY_TEXT_KEY, sql)
                .setAttribute(STATEMENT_NAME_KEY, statementName)
                .setAttribute(DB_USER_KEY, dbUser);
        applyMetadataAttributes(builder, statement);
        applyParentSpan(builder, parentSpan);
        return builder.startSpan();
    }

    private Span startBatchSpan(
            String statementName,
            String sql,
            int batchSize,
            Statement<?> statement,
            Span parentSpan) {
        var builder = tracer.spanBuilder(statementName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(DB_SYSTEM_NAME_KEY, DB_SYSTEM)
                .setAttribute(DB_QUERY_TEXT_KEY, sql)
                .setAttribute(STATEMENT_NAME_KEY, statementName)
                .setAttribute(BATCH_SIZE_KEY, (long) batchSize)
                .setAttribute(DB_USER_KEY, dbUser);
        applyMetadataAttributes(builder, statement);
        applyParentSpan(builder, parentSpan);
        return builder.startSpan();
    }

    private void applyMetadataAttributes(SpanBuilder builder, Statement<?> statement) {
        statement.operationName().ifPresent(v -> builder.setAttribute(DB_OPERATION_NAME_KEY, v));
        statement.collectionName().ifPresent(v -> builder.setAttribute(DB_COLLECTION_NAME_KEY, v));
    }

    private void applyParentSpan(SpanBuilder builder, Span parentSpan) {
        if (parentSpan != null) {
            builder.setParent(Context.current().with(parentSpan));
        }
    }

    private void recordDuration(
            String sql,
            String statementName,
            Statement<?> statement,
            double durationSeconds) {
        var attributesBuilder = Attributes.builder()
                .put(DB_SYSTEM_NAME_KEY, DB_SYSTEM)
                .put(DB_QUERY_TEXT_KEY, sql)
                .put(STATEMENT_NAME_KEY, statementName);
        statement.operationName().ifPresent(v -> attributesBuilder.put(DB_OPERATION_NAME_KEY, v));
        statement.collectionName().ifPresent(v -> attributesBuilder.put(DB_COLLECTION_NAME_KEY, v));
        durationHistogram.record(durationSeconds, attributesBuilder.build());
    }

    private void maybeLogSlowQuery(String statementName, long durationNanos, double durationSeconds) {
        if (Duration.ofNanos(durationNanos).compareTo(slowQueryLogThreshold) > 0) {
            logger.warn("Slow query detected: {} took {} seconds", statementName, durationSeconds);
        }
    }
}
