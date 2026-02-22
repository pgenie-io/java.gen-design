package io.pgenie.example.myspace.musiccatalogue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * An explicit database transaction.
 *
 * <p>Obtained via {@link Session#transaction()} or
 * {@link TransactionBuilder#start()}. Closing the transaction without first
 * calling {@link #commit()} rolls back automatically and restores
 * auto-commit mode on the underlying connection.
 *
 * <pre>{@code
 * try (Transaction tx = session.transaction()) {
 *     tx.execute(new InsertAlbum("name", ...));
 *     tx.commit();
 * }
 * }</pre>
 */
public final class Transaction implements AutoCloseable {

    private final Connection conn;
    private boolean done = false;

    Transaction(Connection conn) {
        this.conn = conn;
    }

    /**
     * Execute a {@link Statement} within this transaction and return its
     * decoded result.
     */
    public <R> R execute(Statement<R> stmt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(stmt.sql())) {
            stmt.bindParams(ps);
            ps.execute();
            return stmt.decodeResult(ps);
        }
    }

    /** Commit all work done in this transaction. */
    public void commit() throws SQLException {
        conn.commit();
        done = true;
    }

    /** Explicitly roll back all work done in this transaction. */
    public void rollback() throws SQLException {
        conn.rollback();
        done = true;
    }

    /**
     * Roll back if not already committed or rolled back, then restore
     * auto-commit mode.
     */
    @Override
    public void close() throws SQLException {
        try {
            if (!done) {
                conn.rollback();
            }
        } finally {
            conn.setAutoCommit(true);
        }
    }
}
