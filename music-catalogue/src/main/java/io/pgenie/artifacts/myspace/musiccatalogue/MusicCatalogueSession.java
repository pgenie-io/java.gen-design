package io.pgenie.artifacts.myspace.musiccatalogue;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import io.codemine.java.postgresql.jdbc.IsolationLevel;
import io.codemine.java.postgresql.jdbc.Statement;
import io.codemine.java.postgresql.jdbc.Transaction;
import io.codemine.java.postgresql.jdbc.TransactionSettings;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.LoggerFactory;

/**
 * Self-contained, production-grade database session for the MusicCatalogue artifact.
 *
 * <p>The session owns a private HikariCP connection pool built from {@link
 * MusicCatalogueConfig}. It exposes a generic {@link #execute(Statement)} method
 * that drives the generated statement records, {@link #executeTransaction(Transaction)}
 * for atomic retry-capable work, checked {@link SQLException}s, OpenTelemetry traces
 * and metrics, SLF4J logging, a health check, and graceful shutdown.
 *
 * <p>Instances are thread-safe; concurrent calls to {@link #execute(Statement)} and
 * {@link #executeTransaction(Transaction)} are supported.
 */
public class MusicCatalogueSession implements AutoCloseable {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MusicCatalogueSession.class);

    private static final String INSTRUMENTATION_SCOPE = "io.pgenie.artifacts.myspace.musiccatalogue";
    private static final String INSTRUMENTATION_VERSION = "1.0.1";
    private static final String DB_SYSTEM = "postgresql";
    private static final String POOL_NAME = "music-catalogue-pool";

    private static final AttributeKey<String> DB_SYSTEM_KEY = AttributeKey.stringKey("db.system");
    private static final AttributeKey<String> POOL_NAME_KEY = AttributeKey.stringKey("pool.name");
    private static final AttributeKey<String> ISOLATION_LEVEL_KEY = AttributeKey.stringKey("db.transaction.isolation_level");
    private static final AttributeKey<Long> MAX_ATTEMPTS_KEY = AttributeKey.longKey("pgenie.transaction.max_attempts");
    private static final AttributeKey<Boolean> READ_ONLY_KEY = AttributeKey.booleanKey("pgenie.transaction.read_only");

    private final MusicCatalogueConfig config;
    private final HikariDataSource hikariDataSource;

    private final Tracer tracer;
    private final Meter meter;
    private final LongCounter transactionRetryCounter;
    private final DoubleHistogram statementDurationHistogram;
    private final StatementExecutor statementExecutor;
    private final List<io.opentelemetry.api.metrics.ObservableLongGauge> observableGauges = new ArrayList<>();

    private final AtomicBoolean closed = new AtomicBoolean(false);

    /**
     * Open a session from the given configuration.
     *
     * <p>The session will own a private HikariCP pool that is torn down when
     * {@link #close()} is called.
     */
    public MusicCatalogueSession(MusicCatalogueConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.hikariDataSource = createHikariDataSource(config);

        OpenTelemetry openTelemetry = config.openTelemetry();
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
        this.statementExecutor = new StatementExecutor(config, tracer, meter, statementDurationHistogram, logger);

        registerPoolGauges();

        logger.info("MusicCatalogueSession opened for jdbcUrl={} user={}", redactUrl(config.jdbcUrl()), config.user());
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
     * The statement span is parented to the current OpenTelemetry span, if any.
     * Any {@link SQLException} is propagated to the caller.
     *
     * @param statement the statement to execute
     * @return the decoded statement result
     * @throws SQLException if a database access error occurs
     */
    public <R> R execute(Statement<R> statement) throws SQLException {
        return execute(statement, Span.current());
    }

    /**
     * Execute any generated statement record with an explicit parent span.
     *
     * <p>The statement is run on a connection borrowed from the internal pool.
     * The emitted statement span will be a child of the supplied {@code parentSpan}.
     * Any {@link SQLException} is propagated to the caller.
     *
     * @param statement  the statement to execute
     * @param parentSpan the parent span for the statement trace
     * @return the decoded statement result
     * @throws SQLException if a database access error occurs
     */
    public <R> R execute(Statement<R> statement, Span parentSpan) throws SQLException {
        Objects.requireNonNull(statement, "statement");
        Objects.requireNonNull(parentSpan, "parentSpan");

        try (Connection connection = hikariDataSource.getConnection()) {
            return statementExecutor.execute(statement, connection, parentSpan);
        }
    }

    /**
     * Execute a transaction using default settings derived from the session configuration.
     *
     * <p>The default isolation level is {@link IsolationLevel#SERIALIZABLE}, the
     * transaction is read-write, and the maximum number of attempts is taken from
     * {@link MusicCatalogueConfig#transactionRetryAttempts()} (clamped to at least 1).
     * The transaction span is parented to the current OpenTelemetry span, if any.
     *
     * @param transaction the transaction to execute
     * @return the transaction result
     * @throws SQLException if a database access error occurs
     */
    public <R> R executeTransaction(Transaction<R> transaction) throws SQLException {
        return executeTransaction(transaction,  Span.current());
    }

    /**
     * Execute a transaction using default settings with an explicit parent span.
     *
     * <p>The default isolation level is {@link IsolationLevel#SERIALIZABLE}, the
     * transaction is read-write, and the maximum number of attempts is taken from
     * {@link MusicCatalogueConfig#transactionRetryAttempts()} (clamped to at least 1).
     * The emitted transaction span will be a child of the supplied {@code parentSpan}.
     *
     * @param transaction the transaction to execute
     * @param parentSpan  the parent span for the transaction trace
     * @return the transaction result
     * @throws SQLException if a database access error occurs
     */
    public <R> R executeTransaction(Transaction<R> transaction, Span parentSpan) throws SQLException {
        return executeTransaction(transaction, new TransactionSettings(
                IsolationLevel.SERIALIZABLE,
                false,
                config.transactionRetryAttempts()), parentSpan);
    }

    /**
     * Execute a transaction with the supplied settings.
     *
     * <p>The transaction body receives an instrumented {@link
     * io.codemine.java.postgresql.jdbc.ExecutionContext} whose statements are
     * traced as children of the transaction span. The number of retries performed
     * by the vendor retry loop is reported via the
     * {@code pgenie.musiccatalogue.transaction.retries} counter.
     *
     * @param transaction the transaction to execute
     * @param settings    the transaction settings
     * @return the transaction result
     * @throws SQLException if a database access error occurs
     */
    public <R> R executeTransaction(Transaction<R> transaction, TransactionSettings settings) throws SQLException {
        return executeTransaction(transaction, settings, Span.current());
    }

    /**
     * Execute a transaction with the supplied settings and an explicit parent span.
     *
     * <p>The transaction body receives an instrumented {@link
     * io.codemine.java.postgresql.jdbc.ExecutionContext} whose statements are
     * traced as children of the transaction span. The emitted transaction span
     * will be a child of the supplied {@code parentSpan}. The number of retries
     * performed by the vendor retry loop is reported via the
     * {@code pgenie.musiccatalogue.transaction.retries} counter.
     *
     * @param transaction the transaction to execute
     * @param settings    the transaction settings
     * @param parentSpan  the parent span for the transaction trace
     * @return the transaction result
     * @throws SQLException if a database access error occurs
     */
    public <R> R executeTransaction(Transaction<R> transaction, TransactionSettings settings, Span parentSpan)
            throws SQLException {
        Objects.requireNonNull(transaction, "transaction");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(parentSpan, "parentSpan");

        Span span = tracer
                .spanBuilder("transaction")
                .setSpanKind(SpanKind.INTERNAL)
                .setParent(parentSpan.storeInContext(io.opentelemetry.context.Context.current()))
                .setAttribute(DB_SYSTEM_KEY, DB_SYSTEM)
                .setAttribute(ISOLATION_LEVEL_KEY, isolationLevelAttribute(settings))
                .setAttribute(MAX_ATTEMPTS_KEY, (long) settings.maxAttempts())
                .setAttribute(READ_ONLY_KEY, settings.readOnly())
                .startSpan();

        try (var scope = span.makeCurrent();
                Connection connection = hikariDataSource.getConnection()) {
            ObservableTransactionContext context = new ObservableTransactionContext(connection, statementExecutor, span);
            R result = transaction.executeOn(context, settings);
            long retries = context.retryCount();
            if (retries > 0) {
                transactionRetryCounter.add(retries);
            }
            span.setStatus(StatusCode.OK);
            return result;
        } catch (Throwable t) {
            span.recordException(t);
            span.setStatus(StatusCode.ERROR, t.getMessage());
            throw t;
        } finally {
            span.end();
        }
    }


    static String isolationLevelAttribute(TransactionSettings settings) {
        return settings.isolationLevel().name();
    }

    /**
     * Perform a short-timeout health check by round-tripping the database.
     *
     * @return {@code true} if the database round-trip succeeds, {@code false}
     * otherwise
     */
    public boolean healthCheck() {
        Span span = tracer
                .spanBuilder("healthCheck")
                .setSpanKind(SpanKind.CLIENT)
                .setAttribute(DB_SYSTEM_KEY, DB_SYSTEM)
                .startSpan();

        try (Connection connection = hikariDataSource.getConnection();
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
            for (var gauge : observableGauges) {
                gauge.close();
            }

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

    static String redactUrl(String url) {
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
}
