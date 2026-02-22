package io.pgenie.example.myspace.musiccatalogue;

import io.pgenie.example.myspace.musiccatalogue.statements.*;
import io.pgenie.example.myspace.musiccatalogue.types.AlbumFormat;
import io.pgenie.example.myspace.musiccatalogue.types.RecordingInfo;
import com.zaxxer.hikari.HikariConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for all generated statements.
 *
 * <p>A PostgreSQL container is started once for the test class. Migrations are
 * applied before any test runs. Each test method obtains a fresh {@link Pool}
 * and {@link Session} so that test data does not bleed between tests (each
 * test uses the same schema but inserts its own rows).
 */
@Testcontainers
class StatementsIT {

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

    @Container
    static final PostgreSQLContainer<?> PG =
            new PostgreSQLContainer<>("postgres:18");

    @BeforeAll
    static void applyMigrations() throws SQLException {
        try (var conn = DriverManager.getConnection(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             var stmt = conn.createStatement()) {
            for (String migration : MIGRATIONS) {
                stmt.execute(migration);
            }
        }
    }

    private Pool pool;

    @BeforeEach
    void createPool() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        cfg.setMaximumPoolSize(2);
        pool = new Pool(cfg);
    }

    // -------------------------------------------------------------------------
    // insert_album
    // -------------------------------------------------------------------------

    @Test
    void insertAlbumReturnsId() throws SQLException {
        try (Session session = pool.session()) {
            var result = session.execute(new InsertAlbum(
                    "Dark Side of the Moon",
                    LocalDate.of(1973, 3, 1),
                    AlbumFormat.VINYL,
                    new RecordingInfo(
                            "Abbey Road",
                            "London",
                            "UK",
                            LocalDate.of(1972, 6, 1)
                    )
            ));
            assertTrue(result.id() > 0, "expected a positive id, got " + result.id());
        }
    }

    @Test
    void insertAlbumWithNullsReturnsId() throws SQLException {
        try (Session session = pool.session()) {
            var result = session.execute(new InsertAlbum(
                    "Untitled",
                    null,
                    null,
                    null
            ));
            assertTrue(result.id() > 0);
        }
    }

    // -------------------------------------------------------------------------
    // select_album_by_format
    // -------------------------------------------------------------------------

    @Test
    void selectAlbumByFormatFindsInsertedAlbum() throws SQLException {
        try (Session session = pool.session()) {
            var recording = new RecordingInfo(
                    "Record Plant",
                    "Sausalito",
                    "USA",
                    LocalDate.of(1976, 8, 1)
            );
            var inserted = session.execute(new InsertAlbum(
                    "Rumours",
                    LocalDate.of(1977, 2, 4),
                    AlbumFormat.CD,
                    recording
            ));

            var rows = session.execute(new SelectAlbumByFormat(AlbumFormat.CD));

            assertTrue(
                    rows.stream().anyMatch(r -> r.id() == inserted.id()),
                    "inserted album not found in result set"
            );
            // Verify the recording round-trips correctly.
            var recordingsFromDb = rows.stream()
                    .filter(r -> r.id() == inserted.id())
                    .map(SelectAlbumByFormat.OutputRow::recording)
                    .toList();
            assertEquals(1, recordingsFromDb.size());
            assertEquals(recording, recordingsFromDb.get(0));
        }
    }

    @Test
    void selectAlbumByFormatReturnsEmptyForAbsentFormat() throws SQLException {
        try (Session session = pool.session()) {
            var rows = session.execute(new SelectAlbumByFormat(AlbumFormat.SACD));
            assertTrue(rows.isEmpty(), "expected no SACD albums in a fresh DB");
        }
    }

    // -------------------------------------------------------------------------
    // select_genre_by_artist
    // -------------------------------------------------------------------------

    @Test
    void selectGenreByArtistReturnsEmptyForUnknownArtist() throws SQLException {
        try (Session session = pool.session()) {
            var rows = session.execute(new SelectGenreByArtist(9999));
            assertTrue(rows.isEmpty());
        }
    }

    @Test
    void selectGenreByArtistSeeded() throws SQLException {
        // Seed the database with data using a direct JDBC connection.
        // (The package only exposes generated statements; DDL/seed queries are
        // executed via raw JDBC — mirroring the Rust test pattern.)
        int artistId;
        try (var conn = DriverManager.getConnection(
                PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
             var stmt = conn.createStatement()) {
            stmt.execute("""
                    INSERT INTO genre (name) VALUES ('Jazz')
                        ON CONFLICT (name) DO NOTHING;
                    INSERT INTO artist (name) VALUES ('Miles Davis');
                    INSERT INTO album (name) VALUES ('Kind of Blue');
                    INSERT INTO album_genre (album, genre)
                        SELECT a.id, g.id FROM album a, genre g
                        WHERE a.name = 'Kind of Blue' AND g.name = 'Jazz';
                    INSERT INTO album_artist (album, artist, "primary")
                        SELECT a.id, ar.id, true FROM album a, artist ar
                        WHERE a.name = 'Kind of Blue' AND ar.name = 'Miles Davis';
                    """);
            try (var rs = stmt.executeQuery(
                    "SELECT id FROM artist WHERE name = 'Miles Davis'")) {
                rs.next();
                artistId = rs.getInt(1);
            }
        }

        try (Session session = pool.session()) {
            var rows = session.execute(new SelectGenreByArtist(artistId));
            assertEquals(1, rows.size());
            assertEquals("Jazz", rows.get(0).name());
        }
    }

    // -------------------------------------------------------------------------
    // update_album_recording_returning
    // -------------------------------------------------------------------------

    @Test
    void updateAlbumRecordingReturningUpdatesAndReturnsRow() throws SQLException {
        try (Session session = pool.session()) {
            var inserted = session.execute(new InsertAlbum(
                    "Wish You Were Here",
                    null,
                    null,
                    null
            ));

            var recording = new RecordingInfo(
                    "EMI",
                    "London",
                    "UK",
                    LocalDate.of(1975, 1, 6)
            );

            var rows = session.execute(
                    new UpdateAlbumRecordingReturning(recording, inserted.id()));

            assertEquals(1, rows.size());
            assertEquals(inserted.id(), rows.get(0).id());
            assertEquals("Wish You Were Here", rows.get(0).name());
            assertEquals(recording, rows.get(0).recording());
        }
    }

    @Test
    void updateAlbumRecordingReturningNoMatchReturnsEmpty() throws SQLException {
        try (Session session = pool.session()) {
            var rows = session.execute(new UpdateAlbumRecordingReturning(null, 99999L));
            assertTrue(rows.isEmpty());
        }
    }

    // -------------------------------------------------------------------------
    // update_album_released
    // -------------------------------------------------------------------------

    @Test
    void updateAlbumReleasedUpdatesRow() throws SQLException {
        try (Session session = pool.session()) {
            var inserted = session.execute(new InsertAlbum(
                    "The Wall",
                    null,
                    null,
                    null
            ));

            LocalDate releaseDate = LocalDate.of(1979, 11, 30);

            session.execute(new UpdateAlbumReleased(releaseDate, inserted.id()));

            // Verify via update_album_recording_returning (returns full row).
            var rows = session.execute(
                    new UpdateAlbumRecordingReturning(null, inserted.id()));

            assertEquals(1, rows.size());
            assertEquals(releaseDate, rows.get(0).released());
        }
    }

    @Test
    void updateAlbumReleasedNoMatchIsNoop() throws SQLException {
        try (Session session = pool.session()) {
            long affected = session.execute(
                    new UpdateAlbumReleased(LocalDate.of(2000, 1, 1), 99999L));
            // 0 rows affected is fine.
            assertEquals(0L, affected);
        }
    }
}
