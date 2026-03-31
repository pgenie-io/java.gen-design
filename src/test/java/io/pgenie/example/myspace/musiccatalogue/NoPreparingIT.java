package io.pgenie.example.myspace.musiccatalogue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.pgenie.example.myspace.musiccatalogue.statements.InsertAlbum;
import io.pgenie.example.myspace.musiccatalogue.statements.SelectAlbumByFormat;
import io.pgenie.example.myspace.musiccatalogue.statements.UpdateAlbumReleased;
import io.pgenie.example.myspace.musiccatalogue.types.AlbumFormat;
import io.pgenie.example.myspace.musiccatalogue.types.RecordingInfo;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link Statement} execution in {@code noPreparing} mode (simple query protocol).
 *
 * <p>
 * {@link #normalModeCreatesServerSidePreparedStatements()} uses the regular
 * datasource from the base class to provide a contrast with the noPreparing datasource.
 */
class NoPreparingIT extends AbstractDatabaseIT {

    private HikariDataSource dsNoPreparing() {
        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(PG.getJdbcUrl());
        cfg.setUsername(PG.getUsername());
        cfg.setPassword(PG.getPassword());
        cfg.setMaximumPoolSize(2);
        cfg.addDataSourceProperty("preferQueryMode", "simple");
        return new HikariDataSource(cfg);
    }

    @Test
    void noPreparingInsertAlbumReturnsId() throws SQLException {
        try (HikariDataSource npDs = dsNoPreparing()) {
            var result = execute(npDs, new InsertAlbum(
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
        try (HikariDataSource npDs = dsNoPreparing()) {
            var recording = new RecordingInfo(
                    "Rockfield Studio",
                    "Monmouth",
                    "UK",
                    LocalDate.of(1975, 1, 1)
            );
            var inserted = execute(npDs, new InsertAlbum(
                    "A Night at the Opera",
                    LocalDate.of(1975, 11, 21),
                    AlbumFormat.CD,
                    recording
            ));

            var rows = execute(npDs, new SelectAlbumByFormat(AlbumFormat.CD));

            assertTrue(
                    rows.stream().anyMatch(r -> r.id() == inserted.id()),
                    "inserted album not found in result set"
            );
        }
    }

    @Test
    void noPreparingUpdateAlbumReleasedNoMatchIsNoop() throws SQLException {
        try (HikariDataSource npDs = dsNoPreparing()) {
            long affected = execute(npDs,
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
        try (HikariDataSource npDs = dsNoPreparing();
                Connection conn = npDs.getConnection()) {
            // Execute above the default prepareThreshold=5 to try to trigger
            // server-side preparation in normal mode.
            for (int i = 0; i < 7; i++) {
                var stmt = new SelectAlbumByFormat(AlbumFormat.SACD);
                try (PreparedStatement ps = conn.prepareStatement(stmt.sql())) {
                    stmt.bindParams(ps);
                    ps.execute();
                    ps.getResultSet().close();
                }
            }
            long count;
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM pg_prepared_statements");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                count = rs.getLong(1);
            }
            assertEquals(0L, count,
                    "noPreparing mode must not create server-side prepared statements");
        }
    }

    @Test
    void normalModeCreatesServerSidePreparedStatements() throws SQLException {
        try (Connection conn = ds.getConnection()) {
            for (int i = 0; i < 7; i++) {
                var stmt = new SelectAlbumByFormat(AlbumFormat.SACD);
                try (PreparedStatement ps = conn.prepareStatement(stmt.sql())) {
                    stmt.bindParams(ps);
                    ps.execute();
                    ps.getResultSet().close();
                }
            }
            long count;
            try (PreparedStatement ps = conn.prepareStatement("SELECT count(*) FROM pg_prepared_statements");
                    ResultSet rs = ps.executeQuery()) {
                rs.next();
                count = rs.getLong(1);
            }
            assertTrue(count > 0,
                    "normal mode should create server-side prepared statements after threshold");
        }
    }
}
