package io.pgenie.artifacts.myspace.musiccatalogue;

import io.codemine.java.postgresql.jdbc.Statement;
import io.codemine.java.postgresql.jdbc.Transaction;
import io.codemine.java.postgresql.jdbc.TransactionSettings;
import io.opentelemetry.api.trace.Span;
import io.pgenie.java.richclient.Session;
import java.sql.SQLException;

/**
 * Thin, backward-compatible wrapper around {@link io.pgenie.java.richclient.Session} for the
 * MusicCatalogue artifact.
 *
 * <p>All implementation details — connection pooling, statement execution, transaction retry,
 * OpenTelemetry traces/metrics and SLF4J logging — are delegated to the shared rich-client
 * library. This class keeps the original public surface so existing call sites and tests continue
 * to compile unchanged.</p>
 */
public class MusicCatalogueSession implements AutoCloseable {

    private final Session session;

    /**
     * Opens a session from the given configuration.
     *
     * <p>The session will own a private HikariCP pool that is torn down when {@link #close()}
     * is called.</p>
     *
     * @param config the MusicCatalogue configuration
     * @throws NullPointerException if {@code config} is null
     */
    public MusicCatalogueSession(MusicCatalogueConfig config) {
        this.session = new Session(config.toRichClientConfig());
    }

    /**
     * Execute any generated statement record.
     *
     * @param statement the statement to execute
     * @return the decoded statement result
     * @throws SQLException if a database access error occurs
     */
    public <R> R execute(Statement<R> statement) throws SQLException {
        return session.execute(statement);
    }

    /**
     * Execute any generated statement record with an explicit parent span.
     *
     * @param statement  the statement to execute
     * @param parentSpan the parent span for the statement trace
     * @return the decoded statement result
     * @throws SQLException if a database access error occurs
     */
    public <R> R execute(Statement<R> statement, Span parentSpan) throws SQLException {
        return session.execute(statement, parentSpan);
    }

    /**
     * Execute a transaction using default settings derived from the session configuration.
     *
     * @param transaction the transaction to execute
     * @return the transaction result
     * @throws SQLException if a database access error occurs
     */
    public <R> R executeTransaction(Transaction<R> transaction) throws SQLException {
        return session.executeTransaction(transaction);
    }

    /**
     * Execute a transaction using default settings with an explicit parent span.
     *
     * @param transaction the transaction to execute
     * @param parentSpan  the parent span for the transaction trace
     * @return the transaction result
     * @throws SQLException if a database access error occurs
     */
    public <R> R executeTransaction(Transaction<R> transaction, Span parentSpan) throws SQLException {
        return session.executeTransaction(transaction, parentSpan);
    }

    /**
     * Execute a transaction with the supplied settings.
     *
     * @param transaction the transaction to execute
     * @param settings    the transaction settings
     * @return the transaction result
     * @throws SQLException if a database access error occurs
     */
    public <R> R executeTransaction(Transaction<R> transaction, TransactionSettings settings) throws SQLException {
        return session.executeTransaction(transaction, settings);
    }

    /**
     * Execute a transaction with the supplied settings and an explicit parent span.
     *
     * @param transaction the transaction to execute
     * @param settings    the transaction settings
     * @param parentSpan  the parent span for the transaction trace
     * @return the transaction result
     * @throws SQLException if a database access error occurs
     */
    public <R> R executeTransaction(
            Transaction<R> transaction,
            TransactionSettings settings,
            Span parentSpan) throws SQLException {
        return session.executeTransaction(transaction, settings, parentSpan);
    }

    /**
     * Perform a short-timeout health check by round-tripping the database.
     *
     * @return {@code true} if the database round-trip succeeds, {@code false} otherwise
     */
    public boolean healthCheck() {
        return session.healthCheck();
    }

    /**
     * Gracefully close the session.
     */
    @Override
    public void close() {
        session.close();
    }
}
