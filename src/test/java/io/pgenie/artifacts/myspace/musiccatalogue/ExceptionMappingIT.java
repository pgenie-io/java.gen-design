package io.pgenie.artifacts.myspace.musiccatalogue;

import static org.junit.jupiter.api.Assertions.*;

import io.codemine.java.postgresql.jdbc.Statement;
import io.pgenie.artifacts.myspace.musiccatalogue.exceptions.ForeignKeyViolationException;
import io.pgenie.artifacts.myspace.musiccatalogue.exceptions.QueryTimeoutException;
import io.pgenie.artifacts.myspace.musiccatalogue.exceptions.UniqueViolationException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ExceptionMappingIT extends AbstractDatabaseIT {

    @Test
    void uniqueViolationIsMapped() {
        String genreName = "Dup Genre " + System.nanoTime();
        session.execute(new InsertGenre(genreName, "dup"));

        UniqueViolationException thrown = assertThrows(
            UniqueViolationException.class,
            () -> session.execute(new InsertGenre(genreName, "dup2"))
        );

        assertInstanceOf(SQLException.class, thrown.getCause());
        String state = ((SQLException) thrown.getCause()).getSQLState();
        assertNotNull(state);
        assertTrue(state.startsWith("23"), "SQLSTATE should be integrity constraint class, got " + state);
    }

    @Test
    void foreignKeyViolationIsMapped() {
        assertThrows(
            ForeignKeyViolationException.class,
            () -> session.execute(new InsertAlbumArtist(999_999L, 999_999L, true))
        );
    }

    @Test
    void queryTimeoutIsMapped() {
        MusicCatalogueConfig shortTimeoutConfig = MusicCatalogueConfig.builder()
            .jdbcUrl(PG.getJdbcUrl())
            .user(PG.getUsername())
            .password(PG.getPassword())
            .maximumPoolSize(2)
            .connectionTimeout(Duration.ofSeconds(5))
            .statementTimeout(Duration.ofSeconds(1))
            .transactionRetryAttempts(3)
            .slowQueryLogThreshold(Duration.ofSeconds(1))
            .build();

        try (var s = new MusicCatalogueSession(shortTimeoutConfig)) {
            QueryTimeoutException thrown = assertThrows(
                QueryTimeoutException.class,
                () -> s.execute(new SleepStatement(5))
            );

            assertInstanceOf(SQLException.class, thrown.getCause());
            String state = ((SQLException) thrown.getCause()).getSQLState();
            assertEquals("57014", state);
        }
    }

    private record InsertGenre(String name, String path) implements Statement<Long> {
        @Override
        public String sql() {
            return "insert into genre (name, path) values (?, ?::ltree)";
        }

        @Override
        public void bindParams(PreparedStatement ps) throws SQLException {
            ps.setString(1, name);
            ps.setString(2, path);
        }

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public Long decodeResultSet(ResultSet rs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long decodeAffectedRows(long affectedRows) {
            return affectedRows;
        }
    }

    private record InsertAlbumArtist(long album, long artist, boolean primary) implements Statement<Long> {
        @Override
        public String sql() {
            return "insert into album_artist (album, artist, \"primary\") values (?, ?, ?)";
        }

        @Override
        public void bindParams(PreparedStatement ps) throws SQLException {
            ps.setLong(1, album);
            ps.setLong(2, artist);
            ps.setBoolean(3, primary);
        }

        @Override
        public boolean returnsRows() {
            return false;
        }

        @Override
        public Long decodeResultSet(ResultSet rs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Long decodeAffectedRows(long affectedRows) {
            return affectedRows;
        }
    }

    private record SleepStatement(int seconds) implements Statement<Long> {
        @Override
        public String sql() {
            return "select pg_sleep(" + seconds + ")";
        }

        @Override
        public void bindParams(PreparedStatement ps) {
            // No parameters.
        }

        @Override
        public boolean returnsRows() {
            return true;
        }

        @Override
        public Long decodeResultSet(ResultSet rs) throws SQLException {
            rs.next();
            return 0L;
        }

        @Override
        public Long decodeAffectedRows(long affectedRows) {
            throw new UnsupportedOperationException();
        }
    }
}
