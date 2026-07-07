package io.pgenie.artifacts.myspace.musiccatalogue;

import static org.junit.jupiter.api.Assertions.*;

import io.codemine.java.postgresql.jdbc.IsolationLevel;
import io.codemine.java.postgresql.jdbc.Statement;
import io.codemine.java.postgresql.jdbc.TransactionSettings;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransactionRetryIT extends AbstractDatabaseIT {

    private InMemoryMetricReader metricReader;
    private SdkMeterProvider meterProvider;
    private OpenTelemetrySdk openTelemetry;

    @BeforeEach
    void createCounterTable() throws SQLException {
        try (Connection conn = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                PreparedStatement ps = conn.prepareStatement("create table if not exists retry_counter (id int primary key, value int not null)")) {
            ps.execute();
        }
        try (Connection conn = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                PreparedStatement ps = conn.prepareStatement("insert into retry_counter (id, value) values (1, 0) on conflict (id) do update set value = 0")) {
            ps.executeUpdate();
        }

        metricReader = InMemoryMetricReader.create();
        meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(metricReader)
                .build();
        openTelemetry = OpenTelemetrySdk.builder()
                .setMeterProvider(meterProvider)
                .build();
    }

    @AfterEach
    void dropCounterTable() throws SQLException {
        try (Connection conn = DriverManager.getConnection(PG.getJdbcUrl(), PG.getUsername(), PG.getPassword());
                PreparedStatement ps = conn.prepareStatement("drop table if exists retry_counter")) {
            ps.execute();
        }
        if (meterProvider != null) {
            meterProvider.forceFlush().join(5, TimeUnit.SECONDS);
            meterProvider.close();
        }
        if (openTelemetry != null) {
            openTelemetry.close();
        }
    }

    @Test
    void serializableConflictsAreRetried() throws SQLException {
        var config = MusicCatalogueConfig.builder()
                .jdbcUrl(PG.getJdbcUrl())
                .user(PG.getUsername())
                .password(PG.getPassword())
                .maximumPoolSize(4)
                .connectionTimeout(Duration.ofSeconds(5))
                .statementTimeout(Duration.ofSeconds(5))
                .transactionRetryAttempts(10)
                .slowQueryLogThreshold(Duration.ofSeconds(1))
                .openTelemetry(openTelemetry)
                .build();

        TransactionSettings settings = TransactionSettings.DEFAULT
                .withIsolationLevel(IsolationLevel.SERIALIZABLE)
                .withMaxAttempts(10);

        AtomicInteger successfulIncrements = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        Callable<Void> increment = () -> {
            try (var retrySession = new MusicCatalogueSession(config)) {
                retrySession.executeTransaction(context -> {
                    int current = context.execute(new SelectCounterStatement());
                    context.execute(new UpdateCounterStatement(current + 1));
                    return null;
                }, settings);
                successfulIncrements.incrementAndGet();
            }
            return null;
        };

        Future<Void> f1 = executor.submit(increment);
        Future<Void> f2 = executor.submit(increment);

        assertDoesNotThrow(() -> f1.get(30, TimeUnit.SECONDS));
        assertDoesNotThrow(() -> f2.get(30, TimeUnit.SECONDS));
        executor.shutdown();

        assertEquals(2, successfulIncrements.get());

        try (var finalSession = new MusicCatalogueSession(config)) {
            int finalValue = finalSession.execute(new SelectCounterStatement());
            assertEquals(2, finalValue);
        }

        meterProvider.forceFlush().join(5, TimeUnit.SECONDS);
        long retryCount = metricReader.collectAllMetrics().stream()
                .filter(metric -> metric.getName().equals("pgenie.musiccatalogue.transaction.retries"))
                .flatMap(metric -> metric.getLongSumData().getPoints().stream())
                .mapToLong(point -> point.getValue())
                .sum();
        assertTrue(retryCount > 0, "expected transaction retries counter to be non-zero, got " + retryCount);
    }

    private record SelectCounterStatement() implements Statement<Integer> {
        @Override
        public String sql() {
            return "select value from retry_counter where id = 1";
        }

        @Override
        public void bindParams(PreparedStatement ps) {}

        @Override
        public boolean returnsRows() {
            return true;
        }

        @Override
        public Integer decodeResultSet(ResultSet rs) throws SQLException {
            rs.next();
            return rs.getInt(1);
        }

        @Override
        public Integer decodeAffectedRows(long affectedRows) {
            throw new UnsupportedOperationException();
        }
    }

    private record UpdateCounterStatement(int value) implements Statement<Long> {
        @Override
        public String sql() {
            return "update retry_counter set value = ? where id = 1";
        }

        @Override
        public void bindParams(PreparedStatement ps) throws SQLException {
            ps.setInt(1, value);
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
}
