package io.pgenie.example.myspace.musiccatalogue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.SQLException;

/**
 * Connection pool for the music-catalogue database.
 *
 * <p>Wraps a {@link HikariDataSource} and vends {@link Session} objects that
 * hold a single pooled {@link java.sql.Connection}. The pool must be
 * {@link #close() closed} when the application shuts down.
 *
 * <p>When {@code noPreparing} is {@code true} the pool configures every
 * connection to use PostgreSQL's simple-query protocol, so statements are
 * executed without server-side preparation.
 */
public final class Pool implements AutoCloseable {

    private final HikariDataSource dataSource;
    private final boolean noPreparing;

    /**
     * Create a pool from a pre-configured {@link HikariConfig}.
     *
     * <p>Example minimal configuration:
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
     * <p>When {@code noPreparing} is {@code true} the PostgreSQL JDBC driver
     * is configured to use the simple-query protocol ({@code preferQueryMode=simple})
     * for all connections, which means statements are never prepared on the
     * server. This is useful in environments where prepared statements are not
     * supported (e.g. PgBouncer in transaction-pooling mode).
     *
     * <p>Note: this method adds a datasource property to {@code config}
     * before the underlying {@link HikariDataSource} is created.
     *
     * @param config      HikariCP configuration (may be mutated when
     *                    {@code noPreparing} is {@code true})
     * @param noPreparing {@code true} to disable server-side statement
     *                    preparation for every connection in this pool
     */
    public Pool(HikariConfig config, boolean noPreparing) {
        if (noPreparing) {
            config.addDataSourceProperty("preferQueryMode", "simple");
        }
        this.dataSource = new HikariDataSource(config);
        this.noPreparing = noPreparing;
    }

    /**
     * Acquire a {@link Session} backed by a pooled connection.
     *
     * <p>The caller is responsible for closing the session (which returns the
     * connection to the pool). Use try-with-resources:
     * <pre>{@code
     * try (Session session = pool.session()) {
     *     var result = session.execute(InsertAlbum.INSTANCE, input);
     * }
     * }</pre>
     */
    public Session session() throws SQLException {
        return new Session(dataSource.getConnection(), noPreparing);
    }

    /** Shut down the pool and release all pooled connections. */
    @Override
    public void close() {
        dataSource.close();
    }
}
