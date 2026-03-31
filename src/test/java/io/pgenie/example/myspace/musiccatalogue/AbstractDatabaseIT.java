package io.pgenie.example.myspace.musiccatalogue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Shared base for all statement integration tests.
 *
 * <p>
 * The PostgreSQL container is started once for the entire JVM run via a static
 * initialiser (singleton container pattern). Schema migrations are applied at
 * that same point. Testcontainers' Ryuk reaper container handles cleanup when
 * the JVM exits, so no explicit {@code stop()} call is needed.
 *
 * <p>
 * Each test method receives a fresh {@link HikariDataSource} (created in {@link #createDataSource}
 * and closed in {@link #closeDataSource}) so that connection state does not bleed
 * between tests.
 */
public abstract class AbstractDatabaseIT {

    // Migrations embedded as string constants, sorted by filename.
    private static final String[] MIGRATIONS = {
            """

                    create table "genre" (
                      "id" int4 not null generated always as identity primary key,
                      "name" text not null unique
                    );

                    create table "artist" (
                      "id" int4 not null generated always as identity primary key,
                      "name" text not null
                    );

                    create table "album" (
                      "id" int4 not null generated always as identity primary key,
                      "name" text not null,
                      "released" date null
                    );

                    create table "album_genre" (
                      "album" int4 not null references "album",
                      "genre" int4 not null references "genre"
                    );

                    create table "album_artist" (
                      "album" int4 not null references "album",
                      "artist" int4 not null references "artist",
                      "primary" bool not null,
                      primary key ("album", "artist")
                    );
                    """,
            """
                    alter table album alter column id type int8;
                    alter table album_genre alter column album type int8;
                    alter table album_artist alter column album type int8;
                    """,
            """
                    create type album_format as enum (
                      'Vinyl', 'CD', 'Cassette', 'Digital', 'DVD-Audio', 'SACD'
                    );
                    create type recording_info as (
                      studio_name text,
                      city text,
                      country text,
                      recorded_date date
                    );
                    alter table album add column format album_format null;
                    alter table album add column recording recording_info null;
                    """
    };

    /** Single container shared across all test classes in the suite. */
    protected static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18");

    static {
        PG.start();
        try {
            applyMigrations();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to apply migrations", e);
        }
    }

    private static void applyMigrations() throws SQLException {
        try (var conn = DriverManager.getConnection(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                var stmt = conn.createStatement()) {
            for (String migration : MIGRATIONS) {
                stmt.execute(migration);
            }
        }
    }

    protected HikariDataSource ds;

    @BeforeEach
    void createDataSource() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        cfg.setMaximumPoolSize(2);
        ds = new HikariDataSource(cfg);
    }

    @AfterEach
    void closeDataSource() {
        ds.close();
    }

    protected static <R> R execute(DataSource source, Statement<R> stmt) throws SQLException {
        try (Connection conn = source.getConnection();
                PreparedStatement ps = conn.prepareStatement(stmt.sql())) {
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

    protected <R> R execute(Statement<R> stmt) throws SQLException {
        return execute(ds, stmt);
    }
}
