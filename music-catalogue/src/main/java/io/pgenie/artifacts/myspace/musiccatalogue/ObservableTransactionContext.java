package io.pgenie.artifacts.myspace.musiccatalogue;

import io.codemine.java.postgresql.jdbc.Statement;
import io.codemine.java.postgresql.jdbc.TransactionContext;
import io.opentelemetry.api.trace.Span;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.List;
import java.util.Objects;

/**
 * Instrumented {@link TransactionContext} used by {@link MusicCatalogueSession}.
 *
 * <p>Delegates statement and batch execution to a shared {@link StatementExecutor}
 * so that spans and metrics emitted inside a transaction are identical to those
 * emitted outside transactions, with the transaction span as their parent.
 * Tracks commit and rollback calls so that the number of transaction retries can
 * be reported after {@link io.codemine.java.postgresql.jdbc.Transaction#executeOn}
 * returns.
 */
final class ObservableTransactionContext implements TransactionContext {

    private final Connection connection;
    private final TransactionContext delegate;
    private final StatementExecutor statementExecutor;
    private final Span transactionSpan;

    private boolean commitCalled;
    private int rollbackCount;

    ObservableTransactionContext(
            Connection connection,
            StatementExecutor statementExecutor,
            Span transactionSpan) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.delegate = TransactionContext.of(connection);
        this.statementExecutor = Objects.requireNonNull(statementExecutor, "statementExecutor");
        this.transactionSpan = Objects.requireNonNull(transactionSpan, "transactionSpan");
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
        commitCalled = true;
    }

    @Override
    public void rollback() throws SQLException {
        delegate.rollback();
        rollbackCount++;
    }

    boolean commitCalled() {
        return commitCalled;
    }

    int rollbackCount() {
        return rollbackCount;
    }
}
