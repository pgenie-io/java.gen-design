package io.pgenie.java.richclient;

import io.codemine.java.postgresql.jdbc.Statement;
import io.codemine.java.postgresql.jdbc.StatementBatch;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.pgenie.java.richclient.observability.SqlOperation;
import io.pgenie.java.richclient.observability.StatementObservability;
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
 * metric and log instrumentation is kept in one place. The actual telemetry logic is delegated
 * to {@link StatementObservability}.</p>
 */
public final class StatementExecutor {

    private final StatementObservability observability;

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
        this.observability = new StatementObservability(
                Objects.requireNonNull(tracer, "tracer"),
                Objects.requireNonNull(meter, "meter"),
                Objects.requireNonNull(logger, "logger"),
                Objects.requireNonNull(dbUser, "dbUser"),
                Objects.requireNonNull(slowQueryLogThreshold, "slowQueryLogThreshold"));
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

        SqlOperation<R> operation = () -> statement.executeOn(connection);
        return observability.observeStatement(statement, parentSpan, operation);
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
        Statement<?> first = statementList.get(0);

        SqlOperation<List<R>> operation = () -> batch.execute(connection);
        return observability.observeBatch(batch.sql(), first, batch.size(), parentSpan, operation);
    }

    private static <R> List<Statement<R>> copyStatements(Iterable<? extends Statement<R>> statements) {
        List<Statement<R>> list = new ArrayList<>();
        statements.forEach(list::add);
        return list;
    }
}
