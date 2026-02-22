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
 */
public final class Pool implements AutoCloseable {

    private final HikariDataSource dataSource;

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
        this.dataSource = new HikariDataSource(config);
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
        return new Session(dataSource.getConnection());
    }

    /** Shut down the pool and release all pooled connections. */
    @Override
    public void close() {
        dataSource.close();
    }
}
