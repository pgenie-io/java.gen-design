package io.pgenie.artifacts.myspace.musiccatalogue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.codemine.java.postgresql.jdbc.Statement;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.slf4j.LoggerFactory;

/**
 * Self-contained, production-grade database session for the MusicCatalogue artifact.
 *
 * <p>The session owns a private HikariCP connection pool built from {@link
 * MusicCatalogueConfig}. It exposes a single generic {@link #execute(Statement)}
 * method that drives the generated statement records, lambda-scoped transactions
 * with automatic retry on serialization failures and deadlocks, unchecked
 * exceptions wrapping the original {@link SQLException}, OpenTelemetry traces
 * and metrics, SLF4J logging, a health check, and graceful shutdown.
 *
 * <p>Instances are thread-safe; concurrent calls to {@link #execute(Statement)}
 * are supported. The session handed to a transaction {@code work} lambda is
 * pinned to a single connection and must not be used concurrently.
 */
public class MusicCatalogueSession implements AutoCloseable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MusicCatalogueSession.class);

    private static final String INSTRUMENTATION_SCOPE = "io.pgenie.artifacts.myspace.musiccatalogue";
    private static final String INSTRUMENTATION_VERSION = "1.0.1";
    private static final String DB_SYSTEM = "postgresql";
    private static final String POOL_NAME = "music-catalogue-pool";

    private static final AttributeKey<String> DB_SYSTEM_KEY = AttributeKey.stringKey("db.system");
    private static final AttributeKey<String> DB_QUERY_TEXT_KEY = AttributeKey.stringKey("db.query.text");
    private static final AttributeKey<String> DB_USER_KEY = AttributeKey.stringKey("db.user");
    private static final AttributeKey<String> STATEMENT_NAME_KEY = AttributeKey.stringKey("pgenie.statement.name");
    private static final AttributeKey<String> POOL_NAME_KEY = AttributeKey.stringKey("pool.name");
    private static final AttributeKey<String> ERROR_TYPE_KEY = AttributeKey.stringKey("error.type");

    private final MusicCatalogueConfig config;
    private final DataSource dataSource;
    private final HikariDataSource hikariDataSource;
    private final boolean ownsConnections;

    private final Tracer tracer;
    private final Meter meter;
    private final LongCounter transactionRetryCounter;
    private final DoubleHistogram statementDurationHistogram;
    private final List<io.opentelemetry.api.metrics.ObservableLongGauge> observableGauges = new ArrayList<>();

    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Open a session from the given configuration.
     *
     * <p>The session will own a private HikariCP pool that is torn down when
     * {@link #close()} is called.
     */
    public MusicCatalogueSession(MusicCatalogueConfig config) {
        this(config, createHikariDataSource(config), true);
    }

    private MusicCatalogueSession(MusicCatalogueConfig config, HikariDataSource hikariDataSource, boolean ownsConnections) {
        this(config, hikariDataSource, hikariDataSource, ownsConnections);
    }

    private MusicCatalogueSession(
            MusicCatalogueConfig config,
            DataSource dataSource,
            HikariDataSource hikariDataSource,
            boolean ownsConnections) {
        this.config = Objects.requireNonNull(config, "config");
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource");
        this.hikariDataSource = hikariDataSource;
        this.ownsConnections = ownsConnections;

        OpenTelemetry openTelemetry = config.openTelemetry() != null ? config.openTelemetry() : GlobalOpenTelemetry.get();
        this.tracer = openTelemetry.getTracer(INSTRUMENTATION_SCOPE, INSTRUMENTATION_VERSION);
        this.meter = openTelemetry.getMeter(INSTRUMENTATION_SCOPE);

        this.transactionRetryCounter = meter
                .counterBuilder("pgenie.musiccatalogue.transaction.retries")
                .setDescription("Number of transaction retries")
                .build();
        this.statementDurationHistogram = meter
                .histogramBuilder("pgenie.musiccatalogue.statement.duration")
                .setDescription("Statement execution duration in seconds")
                .setUnit("s")
                .build();

        if (hikariDataSource != null) {
            registerPoolGauges();
        }

        if (ownsConnections) {
            logger.info("MusicCatalogueSession opened for jdbcUrl={} user={}", redactUrl(config.jdbcUrl()), config.user());
        }
    }

    private static HikariDataSource createHikariDataSource(MusicCatalogueConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.jdbcUrl());
        hikariConfig.setUsername(config.user());
        hikariConfig.setPassword(config.password());
        hikariConfig.setMaximumPoolSize(config.maximumPoolSize());
        hikariConfig.setConnectionTimeout(config.connectionTimeout().toMillis());
        hikariConfig.setPoolName(POOL_NAME);
        return new HikariDataSource(hikariConfig);
    }

    private void registerPoolGauges() {
        Attributes attrs = Attributes.of(POOL_NAME_KEY, POOL_NAME);

        observableGauges.add(meter.gaugeBuilder("pgenie.musiccatalogue.pool.connections.active")
                .setDescription("Active connections in the HikariCP pool")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    HikariPoolMXBean bean = hikariDataSource.getHikariPoolMXBean();
                    if (bean != null) {
                        measurement.record(bean.getActiveConnections(), attrs);
                    }
                }));

        observableGauges.add(meter.gaugeBuilder("pgenie.musiccatalogue.pool.connections.idle")
                .setDescription("Idle connections in the HikariCP pool")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    HikariPoolMXBean bean = hikariDataSource.getHikariPoolMXBean();
                    if (bean != null) {
                        measurement.record(bean.getIdleConnections(), attrs);
                    }
                }));

        observableGauges.add(meter.gaugeBuilder("pgenie.musiccatalogue.pool.connections.pending")
                .setDescription("Threads waiting for a connection from the HikariCP pool")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    HikariPoolMXBean bean = hikariDataSource.getHikariPoolMXBean();
                    if (bean != null) {
                        measurement.record(bean.getThreadsAwaitingConnection(), attrs);
                    }
                }));

        observableGauges.add(meter.gaugeBuilder("pgenie.musiccatalogue.pool.connections.total")
                .setDescription("Total connections in the HikariCP pool")
                .ofLongs()
                .buildWithCallback(measurement -> {
                    HikariPoolMXBean bean = hikariDataSource.getHikariPoolMXBean();
                    if (bean != null) {
                        measurement.record(bean.getTotalConnections(), attrs);
                    }
                }));
    }

    /**
     * Execute any generated statement record.
     *
     * <p>The statement is run on a connection borrowed from the internal pool.
     * Checked {@link SQLException}s are wrapped in an unchecked
     * {@link RuntimeException} with the original exception as the cause.
     */
    public <R> R execute(Statement<R> statement) {
        ensureOpen();
        Objects.requireNonNull(statement, "statement");
        String statementName = statement.getClass().getSimpleName();

        Span span = tracer
                .spanBuilder(statementName)
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(DB_SYSTEM_KEY, DB_SYSTEM)
                .setAttribute(DB_QUERY_TEXT_KEY, statement.sql())
                .setAttribute(STATEMENT_NAME_KEY, statementName)
                .setAttribute(DB_USER_KEY, config.user())
                .startSpan();

        long startNanos = System.nanoTime();
        Connection connection = null;
        try (var scope = span.makeCurrent()) {
            connection = dataSource.getConnection();
            R result = executeStatement(statement, connection);
            span.setStatus(StatusCode.OK);
            return result;
        } catch (SQLException e) {
            RuntimeException wrapped = new RuntimeException(e);
            span.recordException(wrapped);
            span.setStatus(StatusCode.ERROR, wrapped.getMessage());
            throw wrapped;
        } finally {
            if (ownsConnections) {
                closeQuietly(connection);
            }
            long durationNanos = System.nanoTime() - startNanos;
            double durationSeconds = durationNanos / 1_000_000_000.0;
            statementDurationHistogram.record(
                    durationSeconds,
                    Attributes.of(DB_QUERY_TEXT_KEY, statement.sql(), STATEMENT_NAME_KEY, statementName));
            if (Duration.ofNanos(durationNanos).compareTo(config.slowQueryLogThreshold()) > 0) {
                logger.warn("Slow query detected: {} took {} seconds", statementName, durationSeconds);
            }
            span.end();
        }
    }

    private <R> R executeStatement(Statement<R> statement, Connection connection) throws SQLException {
        try (PreparedStatement preparedStatement = connection.prepareStatement(statement.sql())) {
            int timeoutSeconds = (int) config.statementTimeout().toSeconds();
            if (timeoutSeconds > 0) {
                preparedStatement.setQueryTimeout(timeoutSeconds);
            }
            statement.bindParams(preparedStatement);

            if (statement.returnsRows()) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    return statement.decodeResultSet(resultSet);
                }
            } else {
                long affectedRows = preparedStatement.executeLargeUpdate();
                return statement.decodeAffectedRows(affectedRows);
            }
        }
    }

    /**
     * Run a unit of work inside a lambda-scoped transaction.
     *
     * <p>The lambda receives a transaction-scoped session whose {@link
     * #execute(Statement)} calls run on the same pinned connection. The
     * transaction commits if the lambda returns normally and rolls back if it
     * throws. Serialization failures ({@code SQLSTATE 40001}) and deadlocks
     * ({@code SQLSTATE 40P01}) are retried with exponential backoff and jitter
     * up to {@link MusicCatalogueConfig#transactionRetryAttempts()} attempts.
     *
     * <p>The lambda must be free of non-database side effects because it may be
     * executed more than once.
     */
    public <R> R transaction(Function<MusicCatalogueSession, R> work) {
        return transaction(Connection.TRANSACTION_SERIALIZABLE, work);
    }

    /**
     * Run a unit of work inside a lambda-scoped transaction with the given
     * isolation level.
     *
     * @param isolationLevel one of the {@link Connection} transaction isolation
     *                       constants, e.g. {@link Connection#TRANSACTION_SERIALIZABLE}
     */
    public <R> R transaction(int isolationLevel, Function<MusicCatalogueSession, R> work) {
        ensureOpen();
        Objects.requireNonNull(work, "work");
        int maxAttempts = Math.max(1, config.transactionRetryAttempts());

        for (int attempt = 1; ; attempt++) {
            Span span = tracer
                    .spanBuilder("transaction")
                    .setSpanKind(SpanKind.INTERNAL)
                    .setAttribute(DB_SYSTEM_KEY, DB_SYSTEM)
                    .startSpan();

            Connection connection = null;
            boolean originalAutoCommit = true;
            int originalIsolation = Connection.TRANSACTION_READ_COMMITTED;
            try (var scope = span.makeCurrent()) {
                connection = dataSource.getConnection();
                originalAutoCommit = connection.getAutoCommit();
                originalIsolation = connection.getTransactionIsolation();

                connection.setAutoCommit(false);
                connection.setTransactionIsolation(isolationLevel);

                MusicCatalogueSession pinnedSession = new MusicCatalogueSession(
                        config, new SingleConnectionDataSource(connection), null, false);

                R result = work.apply(pinnedSession);
                connection.commit();
                span.setStatus(StatusCode.OK);
                return result;
            } catch (SQLException e) {
                RuntimeException wrapped = new RuntimeException(e);
                span.recordException(wrapped);
                span.setStatus(StatusCode.ERROR, wrapped.getMessage());

                rollbackQuietly(connection);

                if (isRetryable(e) && attempt < maxAttempts) {
                    transactionRetryCounter.add(
                            1, Attributes.of(ERROR_TYPE_KEY, e.getClass().getSimpleName()));
                    logger.warn(
                            "Transaction attempt {}/{} failed with retryable SQLSTATE {}; retrying",
                            attempt,
                            maxAttempts,
                            e.getSQLState());
                    restoreAndCloseQuietly(connection, originalAutoCommit, originalIsolation);
                    span.end();
                    sleepWithInterruptHandling(retryDelayMillis(attempt));
                    continue;
                }

                throw wrapped;
            } catch (RuntimeException e) {
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, e.getMessage());
                rollbackQuietly(connection);

                SQLException sqlCause = e.getCause() instanceof SQLException ? (SQLException) e.getCause() : null;
                if (sqlCause != null && isRetryable(sqlCause) && attempt < maxAttempts) {
                    transactionRetryCounter.add(
                            1, Attributes.of(ERROR_TYPE_KEY, e.getClass().getSimpleName()));
                    logger.warn(
                            "Transaction attempt {}/{} failed with retryable SQLSTATE {}; retrying",
                            attempt,
                            maxAttempts,
                            sqlCause.getSQLState());
                    restoreAndCloseQuietly(connection, originalAutoCommit, originalIsolation);
                    span.end();
                    sleepWithInterruptHandling(retryDelayMillis(attempt));
                    continue;
                }

                throw e;
            } catch (Error e) {
                span.recordException(e);
                span.setStatus(StatusCode.ERROR, e.getMessage());
                rollbackQuietly(connection);
                throw e;
            } finally {
                restoreAndCloseQuietly(connection, originalAutoCommit, originalIsolation);
                span.end();
            }
        }
    }

    private static boolean isRetryable(SQLException e) {
        String state = e.getSQLState();
        return "40001".equals(state) || "40P01".equals(state);
    }

    private static long retryDelayMillis(int attempt) {
        int shift = Math.min(attempt - 1, 10);
        long base = Math.min(50L * (1L << shift), 5_000L);
        long half = Math.max(1L, base / 2);
        return half + ThreadLocalRandom.current().nextLong(half + 1);
    }

    private static void sleepWithInterruptHandling(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Perform a short-timeout health check by round-tripping the database.
     *
     * @return {@code true} if the database round-trip succeeds, {@code false}
     * otherwise
     */
    public boolean healthCheck() {
        ensureOpen();
        Span span = tracer
                .spanBuilder("healthCheck")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(DB_SYSTEM_KEY, DB_SYSTEM)
                .startSpan();

        try (Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement("select 1")) {
            preparedStatement.setQueryTimeout(2);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                boolean ok = resultSet.next();
                span.setStatus(StatusCode.OK);
                return ok;
            }
        } catch (SQLException e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            return false;
        } finally {
            span.end();
        }
    }

    /**
     * Gracefully close the session.
     *
     * <p>Waits up to a deadline for in-flight statements to finish, then tears
     * down the internal HikariCP pool.
     */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        logger.info("Closing MusicCatalogueSession");
        Span span = tracer.spanBuilder("musiccatalogue.session.close").startSpan();
        try {
            if (hikariDataSource != null && !hikariDataSource.isClosed()) {
                HikariPoolMXBean pool = hikariDataSource.getHikariPoolMXBean();
                Instant deadline = Instant.now().plus(Duration.ofSeconds(10));

                while (pool.getActiveConnections() > 0 && Instant.now().isBefore(deadline)) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                int remaining = pool.getActiveConnections();
                if (remaining > 0) {
                    logger.warn("{} active connection(s) remained at close deadline", remaining);
                }

                hikariDataSource.close();
            }
        } finally {
            span.end();
        }
        logger.info("MusicCatalogueSession closed");
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("MusicCatalogueSession is closed");
        }
    }

    private static void rollbackQuietly(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {
                // best effort
            }
        }
    }

    private static void restoreAndCloseQuietly(Connection connection, boolean autoCommit, int isolation) {
        if (connection != null) {
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException ignored) {
                // best effort
            }
            try {
                connection.setTransactionIsolation(isolation);
            } catch (SQLException ignored) {
                // best effort
            }
            closeQuietly(connection);
        }
    }

    private static void closeQuietly(AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception ignored) {
                // best effort
            }
        }
    }

    private static String redactUrl(String url) {
        if (url == null) {
            return null;
        }
        int passwordIndex = url.toLowerCase().indexOf("password=");
        if (passwordIndex == -1) {
            return url;
        }
        int ampersandIndex = url.indexOf('&', passwordIndex);
        if (ampersandIndex == -1) {
            return url.substring(0, passwordIndex + 9) + "***";
        }
        return url.substring(0, passwordIndex + 9) + "***" + url.substring(ampersandIndex);
    }

    /**
     * Minimal {@link DataSource} that always returns the same pinned connection.
     */
    private static final class SingleConnectionDataSource implements DataSource {

        private final Connection connection;

        SingleConnectionDataSource(Connection connection) {
            this.connection = Objects.requireNonNull(connection, "connection");
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public Connection getConnection(String username, String password) {
            return connection;
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {}

        @Override
        public void setLoginTimeout(int seconds) {}

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Not a wrapper");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return false;
        }
    }
}
