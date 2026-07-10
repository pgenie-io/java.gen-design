package io.pgenie.java.richclient;

import com.zaxxer.hikari.HikariPoolMXBean;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongGauge;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Hand-rolled OpenTelemetry gauges that poll a {@link HikariPoolMXBean}.
 *
 * <p>Emits the four {@code pgenie.pool.connections.*} series required by the
 * pgenie observability spec, each tagged with {@code pool.name}. The callbacks
 * are registered when this object is constructed and unregistered when
 * {@link #close()} is called.</p>
 *
 * <p>This is intentionally not the {@code opentelemetry-hikaricp-3.0}
 * integration: the spec keeps the metric shape and hand-rolled polling
 * mechanism unchanged.</p>
 */
public final class PoolMetrics implements AutoCloseable {

    /** Name of the active-connections gauge. */
    private static final String ACTIVE_GAUGE = "pgenie.pool.connections.active";

    /** Name of the idle-connections gauge. */
    private static final String IDLE_GAUGE = "pgenie.pool.connections.idle";

    /** Name of the pending-threads gauge. */
    private static final String PENDING_GAUGE = "pgenie.pool.connections.pending";

    /** Name of the total-connections gauge. */
    private static final String TOTAL_GAUGE = "pgenie.pool.connections.total";

    /** Attribute key used for the pool name. */
    private static final String POOL_NAME_KEY = "pool.name";

    private final Meter meter;
    private final HikariPoolMXBean poolMxBean;
    private final String poolName;
    private final List<ObservableLongGauge> gauges;
    private final AtomicBoolean closed;

    /**
     * Registers pool-metric gauges on the supplied meter.
     *
     * @param meter the OpenTelemetry meter used to build the gauges
     * @param poolMxBean the HikariCP pool MXBean to poll
     * @param poolName the value of the {@code pool.name} attribute on every gauge
     * @throws NullPointerException if any argument is null
     */
    public PoolMetrics(Meter meter, HikariPoolMXBean poolMxBean, String poolName) {
        this.meter = Objects.requireNonNull(meter, "meter");
        this.poolMxBean = Objects.requireNonNull(poolMxBean, "poolMxBean");
        this.poolName = Objects.requireNonNull(poolName, "poolName");
        this.gauges = new ArrayList<>(4);
        this.closed = new AtomicBoolean(false);

        Attributes attributes = Attributes.of(io.opentelemetry.api.common.AttributeKey.stringKey(POOL_NAME_KEY), poolName);

        gauges.add(meter.gaugeBuilder(ACTIVE_GAUGE)
                .setDescription("Active connections in the HikariCP pool")
                .ofLongs()
                .buildWithCallback(measurement -> measurement.record(poolMxBean.getActiveConnections(), attributes)));

        gauges.add(meter.gaugeBuilder(IDLE_GAUGE)
                .setDescription("Idle connections in the HikariCP pool")
                .ofLongs()
                .buildWithCallback(measurement -> measurement.record(poolMxBean.getIdleConnections(), attributes)));

        gauges.add(meter.gaugeBuilder(PENDING_GAUGE)
                .setDescription("Threads waiting for a connection from the HikariCP pool")
                .ofLongs()
                .buildWithCallback(measurement -> measurement.record(poolMxBean.getThreadsAwaitingConnection(), attributes)));

        gauges.add(meter.gaugeBuilder(TOTAL_GAUGE)
                .setDescription("Total connections in the HikariCP pool")
                .ofLongs()
                .buildWithCallback(measurement -> measurement.record(poolMxBean.getTotalConnections(), attributes)));
    }

    /**
     * Unregisters the gauges registered by this instance.
     *
     * <p>Calling this method more than once is safe and has no effect on
     * subsequent calls.</p>
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        for (ObservableLongGauge gauge : gauges) {
            gauge.close();
        }
    }
}
