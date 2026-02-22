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
     *
     * <p>When {@link Statement#returnsRows()} is {@code true} the statement is
     * run with {@link PreparedStatement#execute()} so the result set is
     * accessible via {@link PreparedStatement#getResultSet()}.  Otherwise
     * {@link PreparedStatement#executeUpdate()} is used and the affected-row
     * count is forwarded to {@link Statement#decodeResult}.
     */
    public <R> R execute(Statement<R> stmt) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(stmt.sql())) {
            stmt.bindParams(ps);
            long affectedRows;
            if (stmt.returnsRows()) {
                ps.execute();
                affectedRows = 0;
            } else {
                affectedRows = ps.executeUpdate();
            }
            return stmt.decodeResult(ps, affectedRows);
        }
    }

    /** Return the connection to the pool. */
    @Override
    public void close() throws SQLException {
        conn.close();
    }
}
