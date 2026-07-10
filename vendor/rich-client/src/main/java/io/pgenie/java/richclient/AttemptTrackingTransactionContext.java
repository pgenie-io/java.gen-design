package io.pgenie.java.richclient;

import io.codemine.java.postgresql.jdbc.ExecutionContext;
import io.codemine.java.postgresql.jdbc.Statement;
import io.codemine.java.postgresql.jdbc.TransactionContext;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Objects;

/**
 * Decorator around a {@link TransactionContext} that tracks how many times the vendor retry loop
 * attempted to commit or roll back the current transaction.
 *
 * <p>The counters are updated even when the delegate throws, fixing the under-counting race that
 * occurs when bookkeeping is only performed after a successful delegate call.</p>
 */
public final class AttemptTrackingTransactionContext implements TransactionContext {

    private final TransactionContext delegate;

    private boolean commitCalled;
    private int rollbackCount;

    /**
     * Wraps {@code delegate} in an attempt-tracking decorator.
     *
     * @param delegate the context to delegate to
     * @throws NullPointerException if {@code delegate} is null
     */
    public AttemptTrackingTransactionContext(TransactionContext delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    /**
     * Convenience factory that wraps a fresh {@link TransactionContext#of(Connection)} for the given
     * connection.
     *
     * @param connection the JDBC connection to wrap
     * @return a new attempt-tracking context
     * @throws NullPointerException if {@code connection} is null
     */
    public static AttemptTrackingTransactionContext of(Connection connection) {
        Objects.requireNonNull(connection, "connection");
        return new AttemptTrackingTransactionContext(TransactionContext.of(connection));
    }

    @Override
    public <R> R execute(Statement<R> statement) throws SQLException {
        return delegate.execute(statement);
    }

    @Override
    public <R> List<R> executeBatch(Iterable<? extends Statement<R>> statements) throws SQLException {
        return delegate.executeBatch(statements);
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return delegate.setSavepoint();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        delegate.rollback(savepoint);
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        delegate.releaseSavepoint(savepoint);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return delegate.getAutoCommit();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        delegate.setAutoCommit(autoCommit);
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        return delegate.getTransactionIsolation();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        delegate.setTransactionIsolation(level);
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        return delegate.isReadOnly();
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        delegate.setReadOnly(readOnly);
    }

    @Override
    public void commit() throws SQLException {
        commitCalled = true;
        delegate.commit();
    }

    @Override
    public void rollback() throws SQLException {
        rollbackCount++;
        delegate.rollback();
    }

    /**
     * Returns whether a {@link #commit()} call was attempted, regardless of whether it succeeded.
     *
     * @return true if commit was attempted
     */
    public boolean committed() {
        return commitCalled;
    }

    /**
     * Returns the number of times {@link #rollback()} was attempted, including attempts where the
     * delegate threw.
     *
     * @return the rollback attempt count
     */
    public int rollbackCount() {
        return rollbackCount;
    }

    /**
     * Returns true if {@code failure} is a retryable PostgreSQL failure.
     *
     * <p>Retryable SQLSTATEs are {@code 40001} (serialization failure), {@code 40P01} (deadlock
     * detected), and {@code 23505} (unique violation, which PostgreSQL may raise instead of
     * {@code 40001} under {@code SERIALIZABLE} isolation).</p>
     *
     * @param failure the exception to inspect; may be null
     * @return true if the exception is non-null and has a retryable SQLSTATE
     */
    public static boolean isRetryableFailure(SQLException failure) {
        if (failure == null) {
            return false;
        }
        String state = failure.getSQLState();
        return state != null
                && (state.equals("40001") || state.equals("40P01") || state.equals("23505"));
    }
}
