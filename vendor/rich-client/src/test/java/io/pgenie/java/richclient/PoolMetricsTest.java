package io.pgenie.java.richclient;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.zaxxer.hikari.HikariPoolMXBean;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;

import java.util.Collection;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PoolMetricsTest {

    private static final String POOL_NAME = "test-pool";
    private static final AttributeKey<String> POOL_NAME_KEY = AttributeKey.stringKey("pool.name");

    private final InMemoryMetricReader metricReader = InMemoryMetricReader.create();
    private SdkMeterProvider meterProvider;
    private Meter meter;
    private PoolMetrics poolMetrics;

    @BeforeEach
    void setUp() {
        meterProvider = SdkMeterProvider.builder()
                .registerMetricReader(metricReader)
                .build();
        meter = meterProvider.get("io.pgenie.java.rich-client-test");
    }

    @AfterEach
    void tearDown() {
        if (poolMetrics != null) {
            poolMetrics.close();
        }
        meterProvider.close();
    }

    @Test
    void registersFourPoolGaugesWithValuesAndPoolNameAttribute() {
        poolMetrics = new PoolMetrics(meter, new StubHikariPoolMXBean(1, 2, 3, 4), POOL_NAME);

        var metrics = metricReader.collectAllMetrics();

        assertGauge(metrics, "pgenie.pool.connections.active", 1);
        assertGauge(metrics, "pgenie.pool.connections.idle", 2);
        assertGauge(metrics, "pgenie.pool.connections.pending", 3);
        assertGauge(metrics, "pgenie.pool.connections.total", 4);
    }

    @Test
    void closeUnregistersAllCallbacks() {
        poolMetrics = new PoolMetrics(meter, new StubHikariPoolMXBean(5, 6, 7, 8), POOL_NAME);

        // Ensure the gauges were previously collecting.
        assertGauge(metricReader.collectAllMetrics(), "pgenie.pool.connections.active", 5);

        poolMetrics.close();

        var metrics = metricReader.collectAllMetrics();
        assertEquals(0, metrics.size(), "Expected no metrics after close()");
    }

    @Test
    void closeIsIdempotent() {
        poolMetrics = new PoolMetrics(meter, new StubHikariPoolMXBean(9, 10, 11, 12), POOL_NAME);

        poolMetrics.close();
        poolMetrics.close();

        var metrics = metricReader.collectAllMetrics();
        assertEquals(0, metrics.size(), "Expected no metrics after repeated close()");
    }

    private void assertGauge(Collection<io.opentelemetry.sdk.metrics.data.MetricData> metrics,
            String name, long expectedValue) {
        var metric = metrics.stream()
                .filter(m -> m.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("Missing metric: " + name));

        var points = metric.getLongGaugeData().getPoints();
        assertEquals(1, points.size(), "Expected exactly one point for " + name);

        var point = points.stream().findFirst().orElseThrow();
        assertEquals(expectedValue, point.getValue(), "Wrong value for " + name);
        assertEquals(POOL_NAME, point.getAttributes().get(POOL_NAME_KEY), "Wrong pool.name for " + name);
    }

    /**
     * Hand-written stub of {@link HikariPoolMXBean}; no mocking library is used.
     */
    private static final class StubHikariPoolMXBean implements HikariPoolMXBean {

        private final int active;
        private final int idle;
        private final int pending;
        private final int total;

        StubHikariPoolMXBean(int active, int idle, int pending, int total) {
            this.active = active;
            this.idle = idle;
            this.pending = pending;
            this.total = total;
        }

        @Override
        public int getActiveConnections() {
            return active;
        }

        @Override
        public int getIdleConnections() {
            return idle;
        }

        @Override
        public int getTotalConnections() {
            return total;
        }

        @Override
        public int getThreadsAwaitingConnection() {
            return pending;
        }

        @Override
        public void softEvictConnections() {
            // Stub: no-op.
        }

        @Override
        public void suspendPool() {
            // Stub: no-op.
        }

        @Override
        public void resumePool() {
            // Stub: no-op.
        }
    }
}
