package io.pgenie.artifacts.myspace.musiccatalogue;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.pgenie.java.richclient.RichClientConfig;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for a {@code MusicCatalogueSession}.
 *
 * <p>The JDBC URL identifies the database host, port and name; credentials are supplied
 * separately so that they can be rotated, stored in secret managers, or overridden without
 * touching the connection string.</p>
 *
 * @param jdbcUrl the JDBC URL of the PostgreSQL database
 * @param user the database user
 * @param password the database password
 * @param maximumPoolSize maximum number of connections maintained in the HikariCP pool; at least 1
 * @param connectionTimeout maximum time to wait for a connection from the pool before failing; must not be negative
 * @param statementTimeout maximum time a single statement is allowed to execute before being cancelled; zero means no timeout; must not be negative
 * @param transactionRetryAttempts number of times a transaction is retried when a serialization failure or deadlock is detected; at least 1
 * @param slowQueryLogThreshold queries running longer than this threshold are logged as slow queries; zero logs every query; must not be negative
 * @param openTelemetry OpenTelemetry instance used for tracing and metrics
 */
public record MusicCatalogueConfig(
        String jdbcUrl,
        String user,
        String password,
        int maximumPoolSize,
        Duration connectionTimeout,
        Duration statementTimeout,
        int transactionRetryAttempts,
        Duration slowQueryLogThreshold,
        OpenTelemetry openTelemetry) {

    /**
     * Validates the record's components.
     *
     * @throws NullPointerException if any reference-typed component is null
     * @throws IllegalArgumentException if a numeric or duration component violates its stated bound
     */
    public MusicCatalogueConfig {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(connectionTimeout, "connectionTimeout");
        Objects.requireNonNull(statementTimeout, "statementTimeout");
        Objects.requireNonNull(slowQueryLogThreshold, "slowQueryLogThreshold");
        Objects.requireNonNull(openTelemetry, "openTelemetry");
        if (maximumPoolSize < 1) {
            throw new IllegalArgumentException("maximumPoolSize must be at least 1");
        }
        if (connectionTimeout.isNegative()) {
            throw new IllegalArgumentException("connectionTimeout must not be negative");
        }
        if (statementTimeout.isNegative()) {
            throw new IllegalArgumentException("statementTimeout must not be negative");
        }
        if (transactionRetryAttempts < 1) {
            throw new IllegalArgumentException("transactionRetryAttempts must be at least 1");
        }
        if (slowQueryLogThreshold.isNegative()) {
            throw new IllegalArgumentException("slowQueryLogThreshold must not be negative");
        }
    }

    /**
     * Creates a config with the given required fields and default values for everything else:
     * a maximum pool size of 10, a 30-second connection timeout, a 30-second statement timeout,
     * 3 transaction retry attempts, a 1-second slow-query-log threshold, and the global
     * {@link OpenTelemetry} instance.
     *
     * @param jdbcUrl the JDBC URL of the PostgreSQL database
     * @param user the database user
     * @param password the database password
     * @return a fully-populated config
     * @throws NullPointerException if any argument is null
     */
    public static MusicCatalogueConfig defaults(String jdbcUrl, String user, String password) {
        return new MusicCatalogueConfig(
                jdbcUrl,
                user,
                password,
                10,
                Duration.ofSeconds(30),
                Duration.ofSeconds(30),
                3,
                Duration.ofSeconds(1),
                GlobalOpenTelemetry.get());
    }

    /**
     * Returns a copy of this config with the given JDBC URL.
     *
     * @param jdbcUrl the JDBC URL to apply
     * @return a new {@code MusicCatalogueConfig}
     * @throws NullPointerException if {@code jdbcUrl} is null
     */
    public MusicCatalogueConfig withJdbcUrl(String jdbcUrl) {
        return new MusicCatalogueConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry);
    }

    /**
     * Returns a copy of this config with the given database user.
     *
     * @param user the user to apply
     * @return a new {@code MusicCatalogueConfig}
     * @throws NullPointerException if {@code user} is null
     */
    public MusicCatalogueConfig withUser(String user) {
        return new MusicCatalogueConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry);
    }

    /**
     * Returns a copy of this config with the given database password.
     *
     * @param password the password to apply
     * @return a new {@code MusicCatalogueConfig}
     * @throws NullPointerException if {@code password} is null
     */
    public MusicCatalogueConfig withPassword(String password) {
        return new MusicCatalogueConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry);
    }

    /**
     * Returns a copy of this config with the given maximum pool size.
     *
     * @param maximumPoolSize the maximum pool size to apply; at least 1
     * @return a new {@code MusicCatalogueConfig}
     * @throws IllegalArgumentException if {@code maximumPoolSize} is less than 1
     */
    public MusicCatalogueConfig withMaximumPoolSize(int maximumPoolSize) {
        return new MusicCatalogueConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry);
    }

    /**
     * Returns a copy of this config with the given connection timeout.
     *
     * @param connectionTimeout the connection timeout to apply; must not be negative
     * @return a new {@code MusicCatalogueConfig}
     * @throws NullPointerException if {@code connectionTimeout} is null
     * @throws IllegalArgumentException if {@code connectionTimeout} is negative
     */
    public MusicCatalogueConfig withConnectionTimeout(Duration connectionTimeout) {
        return new MusicCatalogueConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry);
    }

    /**
     * Returns a copy of this config with the given statement timeout.
     *
     * @param statementTimeout the statement timeout to apply; zero means no timeout; must not be negative
     * @return a new {@code MusicCatalogueConfig}
     * @throws NullPointerException if {@code statementTimeout} is null
     * @throws IllegalArgumentException if {@code statementTimeout} is negative
     */
    public MusicCatalogueConfig withStatementTimeout(Duration statementTimeout) {
        return new MusicCatalogueConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry);
    }

    /**
     * Returns a copy of this config with the given transaction retry attempts.
     *
     * @param transactionRetryAttempts the number of retry attempts to apply; at least 1
     * @return a new {@code MusicCatalogueConfig}
     * @throws IllegalArgumentException if {@code transactionRetryAttempts} is less than 1
     */
    public MusicCatalogueConfig withTransactionRetryAttempts(int transactionRetryAttempts) {
        return new MusicCatalogueConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry);
    }

    /**
     * Returns a copy of this config with the given slow-query-log threshold.
     *
     * @param slowQueryLogThreshold the threshold to apply; zero logs every query; must not be negative
     * @return a new {@code MusicCatalogueConfig}
     * @throws NullPointerException if {@code slowQueryLogThreshold} is null
     * @throws IllegalArgumentException if {@code slowQueryLogThreshold} is negative
     */
    public MusicCatalogueConfig withSlowQueryLogThreshold(Duration slowQueryLogThreshold) {
        return new MusicCatalogueConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry);
    }

    /**
     * Returns a copy of this config with the given OpenTelemetry instance.
     *
     * @param openTelemetry the OpenTelemetry instance to apply
     * @return a new {@code MusicCatalogueConfig}
     * @throws NullPointerException if {@code openTelemetry} is null
     */
    public MusicCatalogueConfig withOpenTelemetry(OpenTelemetry openTelemetry) {
        return new MusicCatalogueConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry);
    }

    /**
     * Converts this artifact-specific config into the generic {@link RichClientConfig} used by the
     * shared rich-client session implementation, supplying the artifact identity constants.
     *
     * @return a fully-populated {@link RichClientConfig}
     */
    public RichClientConfig toRichClientConfig() {
        return RichClientConfig.defaults(jdbcUrl, user, password)
                .withMaximumPoolSize(maximumPoolSize)
                .withConnectionTimeout(connectionTimeout)
                .withStatementTimeout(statementTimeout)
                .withTransactionRetryAttempts(transactionRetryAttempts)
                .withSlowQueryLogThreshold(slowQueryLogThreshold)
                .withOpenTelemetry(openTelemetry)
                .withScopeName("io.pgenie.artifacts.myspace.musiccatalogue")
                .withScopeVersion("1.0.1")
                .withPoolName("music-catalogue-pool")
                .withArtifactName("music-catalogue");
    }

    /** Redacts {@link #password()} so it can't leak into logs or exception messages. */
    @Override
    public String toString() {
        return "MusicCatalogueConfig["
                + "jdbcUrl=" + jdbcUrl
                + ", user=" + user
                + ", password=***"
                + ", maximumPoolSize=" + maximumPoolSize
                + ", connectionTimeout=" + connectionTimeout
                + ", statementTimeout=" + statementTimeout
                + ", transactionRetryAttempts=" + transactionRetryAttempts
                + ", slowQueryLogThreshold=" + slowQueryLogThreshold
                + ", openTelemetry=" + openTelemetry
                + ']';
    }
}
