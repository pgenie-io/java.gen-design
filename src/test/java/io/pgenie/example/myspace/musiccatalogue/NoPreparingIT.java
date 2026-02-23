package io.pgenie.example.myspace.musiccatalogue;

import com.zaxxer.hikari.HikariConfig;
import io.pgenie.example.myspace.musiccatalogue.statements.InsertAlbum;
import io.pgenie.example.myspace.musiccatalogue.statements.SelectAlbumByFormat;
import io.pgenie.example.myspace.musiccatalogue.statements.UpdateAlbumReleased;
import io.pgenie.example.myspace.musiccatalogue.types.AlbumFormat;
import io.pgenie.example.myspace.musiccatalogue.types.RecordingInfo;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Pool} in {@code noPreparing} mode (simple query protocol).
 *
 * <p>
 * {@link #normalModeCreatesServerSidePreparedStatements()} uses the regular
 * pool from the base class to provide a contrast with the noPreparing pool.
 */
class NoPreparingIT extends AbstractDatabaseIT {

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
    // Statement helper
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
