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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for all generated statements.
 *
 * <p>
 * A PostgreSQL container is started once for the test class. Migrations are
 * applied before any test runs. Each test method obtains a fresh {@link Pool}
 * so that test data does not bleed between tests (each test uses the same
 * schema but inserts its own rows).
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
    static final PostgreSQLContainer<?> PG = new PostgreSQLContainer<>("postgres:18");

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
        var result = pool.execute(new InsertAlbum(
                "Dark Side of the Moon",
                LocalDate.of(1973, 3, 1),
                AlbumFormat.VINYL,
                new RecordingInfo(
                        "Abbey Road",
                        "London",
                        "UK",
                        LocalDate.of(1972, 6, 1))));
        assertTrue(result.id() > 0, "expected a positive id, got " + result.id());
    }

    @Test
    void insertAlbumWithNullsReturnsId() throws SQLException {
        var result = pool.execute(new InsertAlbum(
                "Untitled",
                null,
                null,
                null));
        assertTrue(result.id() > 0);

    }

    // -------------------------------------------------------------------------
    // select_album_by_format
    // -------------------------------------------------------------------------

    @Test
    void selectAlbumByFormatFindsInsertedAlbum() throws SQLException {
        var recording = new RecordingInfo(
                "Record Plant",
                "Sausalito",
                "USA",
                LocalDate.of(1976, 8, 1));
        var inserted = pool.execute(new InsertAlbum(
                "Rumours",
                LocalDate.of(1977, 2, 4),
                AlbumFormat.CD,
                recording));

        var rows = pool.execute(new SelectAlbumByFormat(AlbumFormat.CD));

        assertTrue(
                rows.stream().anyMatch(r -> r.id() == inserted.id()),
                "inserted album not found in result set");
        // Verify the recording round-trips correctly.
        var recordingsFromDb = rows.stream()
                .filter(r -> r.id() == inserted.id())
                .map(SelectAlbumByFormat.OutputRow::recording)
                .toList();
        assertEquals(1, recordingsFromDb.size());
        assertEquals(recording, recordingsFromDb.get(0));
    }

    @Test
    void selectAlbumByFormatReturnsEmptyForAbsentFormat() throws SQLException {
        var rows = pool.execute(new SelectAlbumByFormat(AlbumFormat.SACD));
        assertTrue(rows.isEmpty(), "expected no SACD albums in a fresh DB");
    }

    // -------------------------------------------------------------------------
    // select_genre_by_artist
    // -------------------------------------------------------------------------

    @Test
    void selectGenreByArtistReturnsEmptyForUnknownArtist() throws SQLException {
        var rows = pool.execute(new SelectGenreByArtist(9999));
        assertTrue(rows.isEmpty());
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

        var rows = pool.execute(new SelectGenreByArtist(artistId));
        assertEquals(1, rows.size());
        assertEquals("Jazz", rows.get(0).name());
    }

    // -------------------------------------------------------------------------
    // update_album_recording_returning
    // -------------------------------------------------------------------------

    @Test
    void updateAlbumRecordingReturningUpdatesAndReturnsRow() throws SQLException {
        var inserted = pool.execute(new InsertAlbum(
                "Wish You Were Here",
                null,
                null,
                null));

        var recording = new RecordingInfo(
                "EMI",
                "London",
                "UK",
                LocalDate.of(1975, 1, 6));

        var rows = pool.execute(
                new UpdateAlbumRecordingReturning(recording, inserted.id()));

        assertEquals(1, rows.size());
        assertEquals(inserted.id(), rows.get(0).id());
        assertEquals("Wish You Were Here", rows.get(0).name());
        assertEquals(recording, rows.get(0).recording());
    }

    @Test
    void updateAlbumRecordingReturningNoMatchReturnsEmpty() throws SQLException {
        var rows = pool.execute(new UpdateAlbumRecordingReturning(null, 99999L));
        assertTrue(rows.isEmpty());
    }

    // -------------------------------------------------------------------------
    // update_album_released
    // -------------------------------------------------------------------------

    @Test
    void updateAlbumReleasedUpdatesRow() throws SQLException {
        var inserted = pool.execute(new InsertAlbum(
                "The Wall",
                null,
                null,
                null));

        LocalDate releaseDate = LocalDate.of(1979, 11, 30);

        long affected = pool.execute(new UpdateAlbumReleased(releaseDate, inserted.id()));
        assertEquals(1L, affected, "expected 1 row to be updated");

        // Verify via update_album_recording_returning (returns full row).
        var rows = pool.execute(
                new UpdateAlbumRecordingReturning(null, inserted.id()));

        assertEquals(1, rows.size());
        assertEquals(releaseDate, rows.get(0).released());
    }

    @Test
    void updateAlbumReleasedNoMatchIsNoop() throws SQLException {
        long affected = pool.execute(
                new UpdateAlbumReleased(LocalDate.of(2000, 1, 1), 99999L));
        // 0 rows affected is fine.
        assertEquals(0L, affected);
    }

    // -------------------------------------------------------------------------
    // Pool.execute shorthand
    // -------------------------------------------------------------------------

    @Test
    void poolExecuteInsertsAlbum() throws SQLException {
        var result = pool.execute(new InsertAlbum(
                "Animals",
                LocalDate.of(1977, 1, 23),
                AlbumFormat.VINYL,
                null));
        assertTrue(result.id() > 0, "expected a positive id, got " + result.id());
    }

    // -------------------------------------------------------------------------
    // Transaction: commit path
    // -------------------------------------------------------------------------

    @Test
    void transactCommitInsertsAlbum() throws SQLException {
        long id = pool.transact(ctx -> {
            var output = ctx.execute(new InsertAlbum("Committed Album", null, null, null));
            return TransactionOutcome.commit(output.id());
        });

        assertTrue(id > 0, "expected positive id, got " + id);

        // Verify the insertion was actually persisted.
        var rows = pool.execute(new UpdateAlbumRecordingReturning(null, id));
        assertEquals(1, rows.size(), "album should exist after commit");
        assertEquals("Committed Album", rows.get(0).name());
    }

    // -------------------------------------------------------------------------
    // Transaction: retry on serialisation failure (SQLState 40001)
    // -------------------------------------------------------------------------

    /** Transaction that fails on the first attempt with a serialisation error. */
    private final class RetryOnSerializationFailure implements Transaction<Long> {
        final AtomicInteger attempts = new AtomicInteger(0);

        @Override
        public TransactionOutcome<Long> run(TransactionContext ctx) throws SQLException {
            if (attempts.getAndIncrement() == 0) {
                ctx.execute(new RaiseSerializationFailure());
            }
            var output = ctx.execute(new InsertAlbum("Retried Album", null, null, null));
            return TransactionOutcome.commit(output.id());
        }
    }

    @Test
    void transactRetriesOnSerializationFailure() throws SQLException {
        var tx = new RetryOnSerializationFailure();
        long id = pool.transact(tx);

        assertTrue(id > 0);
        assertEquals(2, tx.attempts.get(), "expected exactly 2 attempts");
    }

    // -------------------------------------------------------------------------
    // Transaction: non-retryable error propagates
    // -------------------------------------------------------------------------

    @Test
    void transactNonRetryableErrorPropagates() {
        assertThrows(SQLException.class, () -> pool.<Void>transact(ctx -> {
            ctx.execute(new RaiseGenericError());
            return TransactionOutcome.commit(null);
        }));
    }

    // -------------------------------------------------------------------------
    // Statement helpers used by transaction tests
    // -------------------------------------------------------------------------

    /**
     * Raises a serialisation failure (SQLState {@code 40001}) via a PL/pgSQL DO
     * block.
     */
    private static final class RaiseSerializationFailure implements Statement<Void> {
        @Override
        public String sql() {
            return "DO $$ BEGIN RAISE EXCEPTION 'simulated' USING ERRCODE = '40001'; END $$";
        }

        @Override
        public void bindParams(PreparedStatement ps) {
        }

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public Void decodeAffectedRows(long affectedRows) {
            return null;
        }

        @Override
        public Void decodeResultSet(ResultSet rs) {
            throw new UnsupportedOperationException();
        }
    }

    /** Raises a generic PL/pgSQL exception (non-retryable). */
    private static final class RaiseGenericError implements Statement<Void> {
        @Override
        public String sql() {
            return "DO $$ BEGIN RAISE EXCEPTION 'generic error'; END $$";
        }

        @Override
        public void bindParams(PreparedStatement ps) {
        }

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public Void decodeAffectedRows(long affectedRows) {
            return null;
        }

        @Override
        public Void decodeResultSet(ResultSet rs) {
            throw new UnsupportedOperationException();
        }
    }

    // -------------------------------------------------------------------------
    // no_preparing mode — same operations succeed when noPreparing=true
    // -------------------------------------------------------------------------

    private Pool poolNoPreparing() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        cfg.setMaximumPoolSize(2);
        return new Pool(cfg, true);
    }

    @Test
    void noPreparingInsertAlbumReturnsId() throws SQLException {
        try (Pool pool = poolNoPreparing()) {
            var result = pool.execute(new InsertAlbum(
                    "Animals",
                    LocalDate.of(1977, 1, 21),
                    AlbumFormat.VINYL,
                    new RecordingInfo(
                            "Britannia Row Studios",
                            "London",
                            "UK",
                            LocalDate.of(1976, 7, 1)
                    )
            ));
            assertTrue(result.id() > 0, "expected a positive id, got " + result.id());
        }
    }

    @Test
    void noPreparingSelectAlbumByFormatFindsInsertedAlbum() throws SQLException {
        try (Pool pool = poolNoPreparing()) {
            var recording = new RecordingInfo(
                    "Rockfield Studio",
                    "Monmouth",
                    "UK",
                    LocalDate.of(1975, 1, 1)
            );
            var inserted = pool.execute(new InsertAlbum(
                    "A Night at the Opera",
                    LocalDate.of(1975, 11, 21),
                    AlbumFormat.CD,
                    recording
            ));

            var rows = pool.execute(new SelectAlbumByFormat(AlbumFormat.CD));

            assertTrue(
                    rows.stream().anyMatch(r -> r.id() == inserted.id()),
                    "inserted album not found in result set"
            );
        }
    }

    @Test
    void noPreparingUpdateAlbumReleasedNoMatchIsNoop() throws SQLException {
        try (Pool pool = poolNoPreparing()) {
            long affected = pool.execute(
                    new UpdateAlbumReleased(LocalDate.of(2000, 1, 1), 99999L));
            assertEquals(0L, affected);
        }
    }

    /**
     * Executes the same parameterised statement repeatedly within a single
     * transaction (keeping the same JDBC connection) and checks whether
     * server-side prepared statements were created.
     *
     * <p>pgjdbc's default {@code prepareThreshold} is 5: after the 5th
     * execution of the same SQL on the same connection the driver sends a
     * {@code Parse} message so the statement appears in
     * {@code pg_prepared_statements}.  In {@code noPreparing} mode (simple
     * query protocol) this must never happen.
     */
    @Test
    void noPreparingDoesNotCreateServerSidePreparedStatements() throws SQLException {
        try (Pool npPool = poolNoPreparing()) {
            long count = npPool.transact(ctx -> {
                // Execute above the default prepareThreshold=5 to trigger
                // server-side preparation in normal mode.
                for (int i = 0; i < 7; i++) {
                    ctx.execute(new SelectAlbumByFormat(AlbumFormat.SACD));
                }
                return TransactionOutcome.commit(
                        ctx.execute(new SelectPreparedStatementsCount()));
            });
            assertEquals(0L, count,
                    "noPreparing mode must not create server-side prepared statements");
        }
    }

    @Test
    void normalModeCreatesServerSidePreparedStatements() throws SQLException {
        long count = pool.transact(ctx -> {
            for (int i = 0; i < 7; i++) {
                ctx.execute(new SelectAlbumByFormat(AlbumFormat.SACD));
            }
            return TransactionOutcome.commit(
                    ctx.execute(new SelectPreparedStatementsCount()));
        });
        assertTrue(count > 0,
                "normal mode should create server-side prepared statements after threshold");
    }

    // -------------------------------------------------------------------------
    // Statement helpers used by noPreparing tests
    // -------------------------------------------------------------------------

    /** Returns the number of server-side prepared statements in the current session. */
    private static final class SelectPreparedStatementsCount implements Statement<Long> {
        @Override
        public String sql() {
            return "SELECT count(*) FROM pg_prepared_statements";
        }

        @Override
        public void bindParams(PreparedStatement ps) {
        }

        @Override
        public boolean returnsRows() {
            return true;
        }

        @Override
        public Long decodeResultSet(ResultSet rs) throws SQLException {
            rs.next();
            return rs.getLong(1);
        }

        @Override
        public Long decodeAffectedRows(long affectedRows) {
            throw new UnsupportedOperationException();
        }
    }
}
