package io.pgenie.example.myspace.musiccatalogue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Connection pool for the music-catalogue database.
 *
 * <p>
 * Wraps a {@link HikariDataSource} and vends {@link Session} objects that hold
 * a single pooled {@link java.sql.Connection}. The pool must be
 * {@link #close() closed} when the application shuts down.
 *
 * <p>
 * When {@code noPreparing} is {@code true} the pool configures every connection
 * to use PostgreSQL's simple-query protocol, so statements are executed without
 * server-side preparation.
 */
public final class Pool implements AutoCloseable {

    private final HikariDataSource dataSource;

    /**
     * Create a pool from a pre-configured {@link HikariConfig}.
     *
     * <p>
     * Example minimal configuration:
     *
     * <pre>{@code
     * HikariConfig cfg = new HikariConfig();
     * cfg.setJdbcUrl("jdbc:postgresql://localhost:5432/postgres");
     * cfg.setUsername("postgres");
     * cfg.setPassword("postgres");
     * Pool pool = new Pool(cfg);
     * }</pre>
     */
    public Pool(HikariConfig config) {
        this(config, false);
    }

    /**
     * Create a pool from a pre-configured {@link HikariConfig}, optionally
     * disabling server-side statement preparation.
     *
     * <p>
     * When {@code noPreparing} is {@code true} the PostgreSQL JDBC driver is
     * configured to use the simple-query protocol
     * ({@code preferQueryMode=simple}) for all connections, which means
     * statements are never prepared on the server. This is useful in
     * environments where prepared statements are not supported (e.g. PgBouncer
     * in transaction-pooling mode).
     *
     * <p>
     * Note: this method adds a datasource property to {@code config} before the
     * underlying {@link HikariDataSource} is created.
     *
     * @param config HikariCP configuration (may be mutated when
     * {@code noPreparing} is {@code true})
     * @param noPreparing {@code true} to disable server-side statement
     * preparation for every connection in this pool
     */
    public Pool(HikariConfig config, boolean noPreparing) {
        if (noPreparing) {
            config.addDataSourceProperty("preferQueryMode", "simple");
        }
        this.dataSource = new HikariDataSource(config);
    }

    /**
     * Execute a {@link Statement} and return its decoded result.
     *
     * <p>
     * The statement is prepared, its parameters are bound, it is executed, and
     * the result is decoded — all within this call.
     *
     * <p>
     * When {@link Statement#returnsRows()} is {@code true} the statement is run
     * with {@link PreparedStatement#execute()} and the result set is forwarded
     * to {@link Statement#decodeResultSet}. Otherwise
     * {@link PreparedStatement#executeUpdate()} is used and the affected-row
     * count is forwarded to {@link Statement#decodeAffectedRows}.
     */
    public <R> R execute(Statement<R> stmt) throws SQLException {
        try (
                Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(stmt.sql())) {
            stmt.bindParams(ps);
            if (stmt.returnsRows()) {
                ps.execute();
                try (ResultSet rs = ps.getResultSet()) {
                    return stmt.decodeResultSet(rs);
                }
            } else {
                long affectedRows = ps.executeUpdate();
                return stmt.decodeAffectedRows(affectedRows);
            }
        }
    }

    /**
     * Execute a transaction, retrying automatically if it aborts due to a
     * serialisation failure (SQLState {@code 40001}) or deadlock (SQLState
     * {@code 40P01}).
     *
     * <p>
     * A single connection is acquired for the duration of all attempts and
     * released when the method returns. The transaction's isolation level,
     * read-only flag, and deferrable flag are configured from the
     * {@link Transaction} implementation's default methods before each attempt.
     *
     * @param transaction the transaction to run
     * @param <R> the result type
     * @return the result returned by the committed (or explicitly rolled-back)
     * transaction
     * @throws SQLException if the transaction fails with a non-retryable error
     */
    public <R> R transact(Transaction<R> transaction) throws SQLException {
        Connection conn = dataSource.getConnection();
        try {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(transaction.isolationLevel().toJdbc());

            while (true) {
                conn.setReadOnly(transaction.readOnly());
                if (transaction.deferrable()) {
                    try (var stmt = conn.createStatement()) {
                        stmt.execute("SET TRANSACTION DEFERRABLE");
                    }
                }

                try {
                    TransactionContext ctx = new TransactionContext(conn);
                    TransactionOutcome<R> outcome = transaction.run(ctx);
                    if (outcome.shouldCommit()) {
                        conn.commit();
                    } else {
                        conn.rollback();
                    }
                    return outcome.result();
                } catch (SQLException e) {
                    conn.rollback();
                    String sqlState = e.getSQLState();
                    if ("40001".equals(sqlState) || "40P01".equals(sqlState)) {
                        // Serialisation failure or deadlock — retry.
                        continue;
                    }
                    throw e;
                }
            }
        } finally {
            try {
                conn.setReadOnly(false);
                conn.setAutoCommit(true);
            } finally {
                conn.close();
            }
        }
    }

    /**
     * Shut down the pool and release all pooled connections.
     */
    @Override
    public void close() {
        dataSource.close();
    }
}
