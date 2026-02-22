package io.pgenie.example.myspace.musiccatalogue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A single database session backed by one pooled {@link Connection}.
 *
 * <p>Closing the session returns the underlying connection to the pool.
 * Use try-with-resources to ensure the connection is always released:
 *
 * <pre>{@code
 * try (Session session = pool.session()) {
 *     var result = session.execute(new InsertAlbum("Dark Side of the Moon", ...));
 * }
 * }</pre>
 */
public final class Session implements AutoCloseable {

    private final Connection conn;

    Session(Connection conn) {
        this.conn = conn;
    }

    /**
     * Execute a {@link Statement} and return its decoded result.
     *
     * <p>The statement is prepared, its parameters are bound, it is executed,
     * and the result is decoded — all within this call.
     */
    public <R> R execute(Statement<R> stmt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(stmt.sql())) {
            stmt.bindParams(ps);
            ps.execute();
            return stmt.decodeResult(ps);
        }
    }

    /**
     * Begin an explicit transaction on this session's connection.
     *
     * <p>Sets auto-commit to {@code false}. The returned {@link Transaction}
     * must be committed or rolled back (closing it without committing will
     * roll back automatically).
     */
    public Transaction transaction() throws SQLException {
        conn.setAutoCommit(false);
        return new Transaction(conn);
    }

    /**
     * Return a {@link TransactionBuilder} for configuring transaction
     * characteristics before starting.
     */
    public TransactionBuilder buildTransaction() {
        return new TransactionBuilder(conn);
    }

    /** Return the connection to the pool. */
    @Override
    public void close() throws SQLException {
        conn.close();
    }
}
