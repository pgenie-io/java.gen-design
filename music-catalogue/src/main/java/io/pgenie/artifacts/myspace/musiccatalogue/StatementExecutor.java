package io.pgenie.artifacts.myspace.musiccatalogue;

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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

/**
 * Shared statement executor used by {@link MusicCatalogueSession} for both
 * transactional and non-transactional statement execution.
 *
 * <p>Emits OpenTelemetry CLIENT spans and a duration histogram per statement or
 * batch. Statement spans are children of the supplied parent span when one is
 * provided (used inside transactions).
 */
final class StatementExecutor {

    private static final String DB_SYSTEM = "postgresql";

    private static final AttributeKey<String> DB_SYSTEM_KEY = AttributeKey.stringKey("db.system");
    private static final AttributeKey<String> DB_QUERY_TEXT_KEY = AttributeKey.stringKey("db.query.text");
    private static final AttributeKey<String> DB_USER_KEY = AttributeKey.stringKey("db.user");
    private static final AttributeKey<String> STATEMENT_NAME_KEY = AttributeKey.stringKey("pgenie.statement.name");
    private static final AttributeKey<Long> BATCH_SIZE_KEY = AttributeKey.longKey("pgenie.statement.batch_size");

    private final MusicCatalogueConfig config;
    private final Tracer tracer;
    private final Meter meter;
    private final DoubleHistogram statementDurationHistogram;
    private final org.slf4j.Logger logger;

    StatementExecutor(
            MusicCatalogueConfig config,
            Tracer tracer,
            Meter meter,
            DoubleHistogram statementDurationHistogram,
            org.slf4j.Logger logger) {
        this.config = Objects.requireNonNull(config, "config");
        this.tracer = Objects.requireNonNull(tracer, "tracer");
        this.meter = Objects.requireNonNull(meter, "meter");
        this.statementDurationHistogram =
                Objects.requireNonNull(statementDurationHistogram, "statementDurationHistogram");
        this.logger = Objects.requireNonNull(logger, "logger");
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
    <R> R execute(Statement<R> statement, Connection connection, Span parentSpan) throws SQLException {
        Objects.requireNonNull(statement, "statement");
        Objects.requireNonNull(connection, "connection");
        String statementName = statement.getClass().getSimpleName();

        Span span = startStatementSpan(statementName, statement.sql(), parentSpan);
        long startNanos = System.nanoTime();
        try (var scope = span.makeCurrent()) {
            R result = executeStatement(statement, connection);
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
            throw t;
        } finally {
            recordDuration(statement.sql(), statementName, 1, startNanos);
            span.end();
        }
    }

    /**
     * Execute a batch of statements on the supplied connection.
     *
     * <p>All statements must share the same SQL text and must not return rows.
     * A single CLIENT span and metric point is emitted for the whole batch.
     *
     * @param statements the statements to execute
     * @param connection the JDBC connection to use
     * @param parentSpan the parent span, or {@code null} for a root CLIENT span
     * @return the decoded results, in the same order as the input statements
     * @throws SQLException if a database access error occurs
     */
    <R> List<R> executeBatch(Iterable<? extends Statement<R>> statements, Connection connection, Span parentSpan)
            throws SQLException {
        Objects.requireNonNull(statements, "statements");
        Objects.requireNonNull(connection, "connection");

        StatementBatch<R> batch = new StatementBatch<>(statements);
        if (batch.size() == 0) {
            return List.of();
        }

        String statementName = "batch";
        Span span = startBatchSpan(statementName, batch.sql(), batch.size(), parentSpan);
        long startNanos = System.nanoTime();
        try (var scope = span.makeCurrent()) {
            List<R> results = batch.execute(connection, (int) config.statementTimeout().toSeconds());
            span.setStatus(StatusCode.OK);
            return results;
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
            throw t;
        } finally {
            recordDuration(batch.sql(), statementName, batch.size(), startNanos);
            span.end();
        }
    }

    Span startStatementSpan(String statementName, String sql, Span parentSpan) {
        var builder = tracer
                .spanBuilder(statementName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(DB_SYSTEM_KEY, DB_SYSTEM)
                .setAttribute(DB_QUERY_TEXT_KEY, sql)
                .setAttribute(STATEMENT_NAME_KEY, statementName)
                .setAttribute(DB_USER_KEY, config.user());
        if (parentSpan != null) {
            builder.setParent(parentSpan.storeInContext(io.opentelemetry.context.Context.current()));
        }
        return builder.startSpan();
    }

    Span startBatchSpan(String statementName, String sql, int batchSize, Span parentSpan) {
        var builder = tracer
                .spanBuilder(statementName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(DB_SYSTEM_KEY, DB_SYSTEM)
                .setAttribute(DB_QUERY_TEXT_KEY, sql)
                .setAttribute(STATEMENT_NAME_KEY, statementName)
                .setAttribute(BATCH_SIZE_KEY, (long) batchSize)
                .setAttribute(DB_USER_KEY, config.user());
        if (parentSpan != null) {
            builder.setParent(parentSpan.storeInContext(io.opentelemetry.context.Context.current()));
        }
        return builder.startSpan();
    }

    private <R> R executeStatement(Statement<R> statement, Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement.sql())) {
            applyQueryTimeout(preparedStatement);
            statement.bindParams(preparedStatement);

            if (statement.returnsRows()) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return statement.decodeResultSet(resultSet);
                }
            } else {
                long affectedRows = preparedStatement.executeLargeUpdate();
                return statement.decodeAffectedRows(affectedRows);
            }
        }
    }

    private void applyQueryTimeout(PreparedStatement preparedStatement) throws SQLException {
        int timeoutSeconds = (int) config.statementTimeout().toSeconds();
        if (timeoutSeconds > 0) {
            preparedStatement.setQueryTimeout(timeoutSeconds);
        }
    }

    private void recordDuration(String sql, String statementName, long batchSize, long startNanos) {
        long durationNanos = System.nanoTime() - startNanos;
        double durationSeconds = durationNanos / 1_000_000_000.0;

        Attributes attributes = batchSize > 1
                ? Attributes.of(
                        DB_QUERY_TEXT_KEY, sql,
                        STATEMENT_NAME_KEY, statementName,
                        BATCH_SIZE_KEY, batchSize)
                : Attributes.of(DB_QUERY_TEXT_KEY, sql, STATEMENT_NAME_KEY, statementName);
        statementDurationHistogram.record(durationSeconds, attributes);

        if (Duration.ofNanos(durationNanos).compareTo(config.slowQueryLogThreshold()) > 0) {
            logger.warn("Slow query detected: {} took {} seconds", statementName, durationSeconds);
        }
    }
}
