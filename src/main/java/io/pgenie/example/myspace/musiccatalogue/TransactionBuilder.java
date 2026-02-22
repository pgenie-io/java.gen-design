package io.pgenie.example.myspace.musiccatalogue;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Fluent builder for configuring a {@link Transaction} before it starts.
 *
 * <p>Obtained via {@link Session#buildTransaction()}.
 *
 * <pre>{@code
 * try (Transaction tx = session.buildTransaction()
 *         .isolationLevel(Connection.TRANSACTION_SERIALIZABLE)
 *         .readOnly(true)
 *         .start()) {
 *     var rows = tx.execute(SelectAlbumByFormat.INSTANCE, input);
 * }
 * }</pre>
 */
public final class TransactionBuilder {

    private final Connection conn;
    private int isolationLevel = Connection.TRANSACTION_READ_COMMITTED;
    private boolean readOnly = false;
    private boolean deferrable = false;

    TransactionBuilder(Connection conn) {
        this.conn = conn;
    }

    /**
     * Set the transaction isolation level.
     *
     * <p>Use one of the {@link Connection}{@code .TRANSACTION_*} constants,
     * e.g. {@link Connection#TRANSACTION_SERIALIZABLE}.
     */
    public TransactionBuilder isolationLevel(int level) {
        this.isolationLevel = level;
        return this;
    }

    /**
     * Mark the transaction as read-only. The database may optimise execution
     * of read-only transactions.
     */
    public TransactionBuilder readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    /**
     * Set whether the transaction is deferrable (only meaningful for
     * {@code SERIALIZABLE} read-only transactions).
     */
    public TransactionBuilder deferrable(boolean deferrable) {
        this.deferrable = deferrable;
        return this;
    }

    /**
     * Apply the configured characteristics and begin the transaction.
     *
     * @return a {@link Transaction} ready for statement execution.
     */
    public Transaction start() throws SQLException {
        conn.setAutoCommit(false);
        conn.setTransactionIsolation(isolationLevel);
        conn.setReadOnly(readOnly);
        if (deferrable) {
            // pgjdbc exposes deferrable via a connection property; the standard
            // JDBC API does not have a direct setDeferrable call.  We issue the
            // SET command manually so callers always get the correct behaviour.
            try (var stmt = conn.createStatement()) {
                stmt.execute("SET TRANSACTION DEFERRABLE");
            }
        }
        return new Transaction(conn);
    }
}
