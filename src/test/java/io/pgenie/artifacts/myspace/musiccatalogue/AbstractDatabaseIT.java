package io.pgenie.artifacts.myspace.musiccatalogue;

import io.codemine.java.postgresql.jdbc.Statement;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Shared base for all statement integration tests.
 *
 * <p>The PostgreSQL container is started once for the entire JVM run via a static
 * initialiser (singleton container pattern). Schema migrations are applied at that
 * same point. Testcontainers' Ryuk reaper container handles cleanup when the JVM
 * exits, so no explicit {@code stop()} call is needed.
 *
 * <p>Each test method receives a fresh {@link MusicCatalogueSession} (created in
 * {@link #openSession} and closed in {@link #closeSession}) so that connection state
 * does not bleed between tests.
 */
public abstract class AbstractDatabaseIT {

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
          -- Album name.
          "name" text not null,
          -- The date the album was first released.
          "released" date null
        );

        create table "album_genre" (
          "album" int4 not null references "album",
          "genre" int4 not null references "genre"
        );

        create table "album_artist" (
          "album" int4 not null references "album",
          "artist" int4 not null references "artist",
          -- Whether it is the primary artist
          "primary" bool not null,
          primary key ("album", "artist")
        );
        """,
        """
        -- In this migration we're changing the type of the album "id" column
        -- from "int4" to "int8".
        -- Since this column is referenced from other tables, we also update them.

        
        alter table album
        alter column id type int8;

        alter table album_genre
        alter column album type int8;

        alter table album_artist
        alter column album type int8;
        """,
        """
        -- Add enumeration type for album formats
        create type album_format as enum (
          'Vinyl',
          'CD',
          'Cassette',
          'Digital',
          'DVD-Audio',
          'SACD'
        );

        -- Add composite type for recording session information
        create type recording_info as (
          studio_name text,
          city text,
          country text,
          recorded_date date
        );

        -- Add format column to album table
        alter table album
        add column format album_format null;

        -- Add recording information to album table
        alter table album
        add column recording recording_info null;
        """,
        """
        -- Composite with array fields: a track in an album
        create type track_info as (
          title text,
          duration_seconds int4,
          tags text[]
        );

        -- Composite with composites: disc information referencing a recording
        create type disc_info as (
          name text,
          recording recording_info
        );

        -- Edge case: array of composites column
        alter table album
        add column tracks track_info[] null;

        -- Edge case: composite with composites column
        alter table album
        add column disc disc_info null;
        """,
        """
        CREATE INDEX ON album (recording);
        """,
        """
        -- Add support for hierarchical genre paths.
        create extension if not exists ltree;

        alter table genre
        add column path ltree not null;

        create index on genre using gist (path);"""
    };

    /** Single container shared across all test classes in the suite. */
    protected static final PostgreSQLContainer<?> PG =
        new PostgreSQLContainer<>("postgres:18")
            .withCommand("postgres", "-c", "max_connections=200");

    static {
        PG.start();
        try {
            applyMigrations();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to apply migrations", e);
        }
    }

    private static void applyMigrations() throws SQLException {
        try (var conn =
                    DriverManager.getConnection(
                            PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                var stmt = conn.createStatement()) {
            for (String migration : MIGRATIONS) {
                stmt.execute(migration);
            }
        }
    }

    protected MusicCatalogueSession session;

    @BeforeEach
    void openSession() {
        MusicCatalogueConfig config = MusicCatalogueConfig
            .builder()
            .jdbcUrl(PG.getJdbcUrl())
            .user(PG.getUsername())
            .password(PG.getPassword())
            .maximumPoolSize(1)
            .connectionTimeout(Duration.ofSeconds(5))
            .statementTimeout(Duration.ofSeconds(5))
            .transactionRetryAttempts(3)
            .slowQueryLogThreshold(Duration.ofSeconds(1))
            .build();
        session = new MusicCatalogueSession(config);
    }

    @AfterEach
    void closeSession() {
        if (session != null) {
            session.close();
        }
    }

    protected <R> R execute(Statement<R> stmt) throws SQLException {
        return session.execute(stmt);
    }
}
