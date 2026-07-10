package io.pgenie.java.richclient;

import io.codemine.java.postgresql.jdbc.Transaction;
import io.codemine.java.postgresql.jdbc.TransactionSettings;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import org.slf4j.Logger;

/**
 * Executes {@link Transaction} instances with OpenTelemetry tracing, retry counting and outcome
 * classification.
 *
 * <p>The executor emits a single INTERNAL {@code "transaction"} span that parents all statement
 * spans created by the transaction body. It reports the number of retries performed by the vendor
 * retry loop via the {@code pgenie.transaction.retries} counter and annotates the span with
 * transaction settings, total attempts and final outcome.</p>
 */
public final class TransactionExecutor {

    private static final String DB_SYSTEM = "postgresql";

    private static final String METRIC_NAME = "pgenie.transaction.retries";
    private static final String METRIC_DESCRIPTION = "Number of transaction retries";

    private static final AttributeKey<String> DB_SYSTEM_NAME_KEY = AttributeKey.stringKey("db.system.name");
    private static final AttributeKey<String> ISOLATION_LEVEL_KEY =
            AttributeKey.stringKey("pgenie.transaction.isolation_level");
    private static final AttributeKey<Long> MAX_ATTEMPTS_KEY =
            AttributeKey.longKey("pgenie.transaction.max_attempts");
    private static final AttributeKey<Boolean> READ_ONLY_KEY =
            AttributeKey.booleanKey("pgenie.transaction.read_only");
    private static final AttributeKey<Long> ATTEMPT_COUNT_KEY =
            AttributeKey.longKey("pgenie.transaction.attempt_count");
    private static final AttributeKey<String> OUTCOME_KEY =
            AttributeKey.stringKey("pgenie.transaction.outcome");

    private static final String OUTCOME_COMMITTED = "committed";
    private static final String OUTCOME_RETRIES_EXHAUSTED = "retries_exhausted";
    private static final String OUTCOME_NON_RETRYABLE_FAILURE = "non_retryable_failure";

    private final Tracer tracer;
    private final StatementExecutor statementExecutor;
    private final LongCounter retriesCounter;
    private final Logger logger;

    /**
     * Creates a new transaction executor.
     *
     * @param tracer           the OpenTelemetry tracer used to create the transaction span
     * @param statementExecutor the statement executor used to run statements/batches under the
     *                         transaction span
     * @param meter            the OpenTelemetry meter used to derive the retries counter
     * @param logger           the SLF4J logger used to warn when retries are exhausted
     * @throws NullPointerException if any argument is null
     */
    public TransactionExecutor(
            Tracer tracer,
            StatementExecutor statementExecutor,
            Meter meter,
            Logger logger) {
        this.tracer = Objects.requireNonNull(tracer, "tracer");
        this.statementExecutor = Objects.requireNonNull(statementExecutor, "statementExecutor");
        Objects.requireNonNull(meter, "meter");
        this.logger = Objects.requireNonNull(logger, "logger");
        this.retriesCounter = meter.counterBuilder(METRIC_NAME)
                .setDescription(METRIC_DESCRIPTION)
                .build();
    }

    /**
     * Executes the supplied transaction with the given settings and connection.
     *
     * <p>A transaction span is started as a child of {@code parentSpan} (or the current span if
     * {@code parentSpan} is {@code null}). Statement execution inside the transaction body is
     * routed through the supplied {@link StatementExecutor} so that statement spans are nested
     * under the transaction span.</p>
     *
     * @param transaction the transaction to execute
     * @param settings    the transaction settings
     * @param connection  the JDBC connection to use
     * @param parentSpan  the parent span, or {@code null} to use the current span
     * @return the transaction result
     * @throws SQLException if a database access error occurs
     */
    public <R> R execute(
            Transaction<R> transaction,
            TransactionSettings settings,
            Connection connection,
            Span parentSpan) throws SQLException {
        Objects.requireNonNull(transaction, "transaction");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(connection, "connection");

        Span span = startTransactionSpan(settings, parentSpan);
        AttemptTrackingTransactionContext tracking = AttemptTrackingTransactionContext.of(connection);
        try (var scope = span.makeCurrent()) {
            InstrumentedTransactionContext context = new InstrumentedTransactionContext(
                    tracking, statementExecutor, span, connection);

            R result = transaction.executeOn(context, settings);

            int attempts = computeAttemptCount(tracking, true);
            span.setAttribute(ATTEMPT_COUNT_KEY, (long) attempts);
            span.setAttribute(OUTCOME_KEY, OUTCOME_COMMITTED);
            retriesCounter.add(Math.max(0, attempts - 1));
            span.setStatus(StatusCode.OK);

            return result;
        } catch (Throwable t) {
            int attempts = computeAttemptCount(tracking, false);
            String outcome = classifyOutcome(t);
            span.setAttribute(ATTEMPT_COUNT_KEY, (long) attempts);
            span.setAttribute(OUTCOME_KEY, outcome);
            retriesCounter.add(Math.max(0, attempts - 1));
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());

            if (OUTCOME_RETRIES_EXHAUSTED.equals(outcome)) {
                logger.warn("Transaction exhausted {} attempts, last failure: {}", settings.maxAttempts(), t);
            }

            throw t;
        } finally {
            span.end();
        }
    }

    private Span startTransactionSpan(TransactionSettings settings, Span parentSpan) {
        var builder = tracer.spanBuilder("transaction")
                .setSpanKind(SpanKind.INTERNAL)
                .setAttribute(DB_SYSTEM_NAME_KEY, DB_SYSTEM)
                .setAttribute(ISOLATION_LEVEL_KEY, settings.isolationLevel().name())
                .setAttribute(MAX_ATTEMPTS_KEY, (long) settings.maxAttempts())
                .setAttribute(READ_ONLY_KEY, settings.readOnly());

        if (parentSpan != null) {
            builder.setParent(Context.current().with(parentSpan));
        }
        // When parentSpan is null, the span builder defaults to the current context,
        // making the span a child of Span.current().

        return builder.startSpan();
    }

    static int computeAttemptCount(AttemptTrackingTransactionContext tracking, boolean success) {
        if (success) {
            // Every rollback preceded a failed attempt; the final attempt succeeded.
            return tracking.rollbackCount() + 1;
        }
        // Each rollback is a failed attempt. If there were no rollbacks (e.g. an Error was thrown
        // before the retry loop could roll back), the failing execution still counts as one attempt.
        return Math.max(1, tracking.rollbackCount());
    }

    private static String classifyOutcome(Throwable t) {
        SQLException sqlException = extractSqlException(t);
        return AttemptTrackingTransactionContext.isRetryableFailure(sqlException)
                ? OUTCOME_RETRIES_EXHAUSTED
                : OUTCOME_NON_RETRYABLE_FAILURE;
    }

    private static SQLException extractSqlException(Throwable t) {
        if (t instanceof SQLException sqlException) {
            return sqlException;
        }
        Throwable cause = t.getCause();
        if (cause instanceof SQLException sqlException) {
            return sqlException;
        }
        return null;
    }
}
