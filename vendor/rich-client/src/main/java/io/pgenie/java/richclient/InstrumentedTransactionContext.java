package io.pgenie.java.richclient;

import io.codemine.java.postgresql.jdbc.ExecutionContext;
import io.codemine.java.postgresql.jdbc.Statement;
import io.codemine.java.postgresql.jdbc.TransactionContext;
import io.opentelemetry.api.trace.Span;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Objects;

/**
 * Decorator around an {@link AttemptTrackingTransactionContext} that routes statement and batch
 * execution through {@link StatementExecutor} so that statement spans are emitted as children of
 * the surrounding transaction span.
 *
 * <p>All other {@link TransactionContext}/{@link ExecutionContext} operations are delegated
 * unchanged to the wrapped tracking context, preserving its commit/rollback bookkeeping.</p>
 */
public final class InstrumentedTransactionContext implements TransactionContext {

    private final AttemptTrackingTransactionContext delegate;
    private final StatementExecutor statementExecutor;
    private final Span transactionSpan;
    private final Connection connection;

    /**
     * Creates a new instrumented transaction context.
     *
     * @param delegate         the attempt-tracking context to delegate to
     * @param statementExecutor the statement executor used to run statements/batches under the
     *                         transaction span
     * @param transactionSpan  the OpenTelemetry span representing the transaction
     * @param connection       the JDBC connection used to execute statements
     * @throws NullPointerException if any argument is null
     */
    public InstrumentedTransactionContext(
            AttemptTrackingTransactionContext delegate,
            StatementExecutor statementExecutor,
            Span transactionSpan,
            Connection connection) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.statementExecutor = Objects.requireNonNull(statementExecutor, "statementExecutor");
        this.transactionSpan = Objects.requireNonNull(transactionSpan, "transactionSpan");
        this.connection = Objects.requireNonNull(connection, "connection");
    }

    @Override
    public <R> R execute(Statement<R> statement) throws SQLException {
        return statementExecutor.execute(statement, connection, transactionSpan);
    }

    @Override
    public <R> List<R> executeBatch(Iterable<? extends Statement<R>> statements) throws SQLException {
        return statementExecutor.executeBatch(statements, connection, transactionSpan);
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
        delegate.commit();
    }

    @Override
    public void rollback() throws SQLException {
        delegate.rollback();
    }
}
