package io.pgenie.artifacts.myspace.musiccatalogue;

import static org.junit.jupiter.api.Assertions.*;

import io.codemine.java.postgresql.jdbc.Statement;
import io.pgenie.java.richclient.RichClientConfig;
import io.pgenie.java.richclient.Session;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;

class GracefulCloseIT extends AbstractDatabaseIT {

    @Test

    void closeDrainsInFlightStatement() throws Exception {
        var config = RichClientConfig
                .defaults(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                .withScopeName("io.pgenie.artifacts.myspace.musiccatalogue")
                .withScopeVersion("1.0.1")
                .withPoolName("music-catalogue-pool")
                .withArtifactName("music-catalogue")
                .withMaximumPoolSize(2)
                .withConnectionTimeout(Duration.ofSeconds(1))
                .withStatementTimeout(Duration.ofSeconds(30));

        try (var closeSession = new Session(config)) {
            CountDownLatch started = new CountDownLatch(1);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<?> future = executor.submit(() -> closeSession.execute(new SleepStatement(20, started)));

            // Wait until the statement has acquired a connection and is about to execute.
            assertDoesNotThrow(
                    () -> assertTrue(started.await(2, TimeUnit.SECONDS), "Statement did not start in time"));

            Instant closeStart = Instant.now();
            assertDoesNotThrow(closeSession::close);
            Duration closeDuration = Duration.between(closeStart, Instant.now());

            // Close must finish within a reasonable deadline even though a statement is in flight.
            assertTrue(
                    closeDuration.compareTo(Duration.ofSeconds(12)) < 0,
                    "Expected close to finish before an excessive wait, but took " + closeDuration);

            // Once the pool is closed the in-flight statement is severed.
            assertThrows(Exception.class, () -> future.get(5, TimeUnit.SECONDS));
            executor.shutdownNow();
        }
    }

    @Test

    void closeIsIdempotent() throws Exception {
        var config = RichClientConfig
                .defaults(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword())
                .withScopeName("io.pgenie.artifacts.myspace.musiccatalogue")
                .withScopeVersion("1.0.1")
                .withPoolName("music-catalogue-pool")
                .withArtifactName("music-catalogue");
        var closeSession = new Session(config);
        assertDoesNotThrow(closeSession::close);
        assertDoesNotThrow(closeSession::close);
        assertThrows(IllegalStateException.class, () -> closeSession.execute(new SleepStatement(1, new CountDownLatch(0))));
    }

    private record SleepStatement(int seconds, CountDownLatch started) implements Statement<Long> {
        @Override
        public String sql() {
            return "select pg_sleep(" + seconds + ")";
        }

        @Override
        public void bindParams(PreparedStatement ps) {
            started.countDown();
        }

        @Override
        public boolean returnsRows() {
            return true;
        }

        @Override
        public Long decodeResultSet(ResultSet rs) throws SQLException {
            rs.next();
            return null;
        }

        @Override
        public Long decodeAffectedRows(long affectedRows) {
            throw new UnsupportedOperationException();
        }
    }
}
