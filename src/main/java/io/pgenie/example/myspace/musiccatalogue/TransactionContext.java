package io.pgenie.example.myspace.musiccatalogue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Execution context passed to {@link Transaction#run} implementations.
 *
 * <p>Wraps the underlying {@link Connection} that is already in a transaction,
 * providing only statement execution.  Commit and rollback are managed by
 * {@link Pool#transact}.
 */
public final class TransactionContext {

    private final Connection conn;

    TransactionContext(Connection conn) {
        this.conn = conn;
    }

    /**
     * Execute a {@link Statement} within the current transaction and return
     * its decoded result.
     *
     * <p>Follows the same {@link Statement#returnsRows()} branching as
     * {@link Session#execute}: row-returning statements use
     * {@link PreparedStatement#execute()}, DML statements use
     * {@link PreparedStatement#executeUpdate()} and forward the affected-row
     * count to {@link Statement#decodeResult}.
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
}
