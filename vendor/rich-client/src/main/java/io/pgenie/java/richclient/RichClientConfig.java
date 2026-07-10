package io.pgenie.java.richclient;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for a rich-client session.
 *
 * <p>The JDBC URL identifies the database host, port and name; credentials are supplied
 * separately so that they can be rotated, stored in secret managers, or overridden without
 * touching the connection string.</p>
 *
 * <p>This single record owns both connection/pooling settings and observability settings
 * (instrumentation scope, pool naming, artifact identity) so that generated artifacts have
 * exactly one config type to construct and pass around.</p>
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
 * @param scopeName the OpenTelemetry instrumentation-scope name, e.g. what {@code openTelemetry.getTracer(scopeName, scopeVersion)} uses
 * @param scopeVersion the OpenTelemetry instrumentation-scope version
 * @param poolName the HikariCP pool name; also used to disambiguate pool metrics across multiple instances
 * @param artifactName the name of the generated artifact this config belongs to, e.g. {@code "music-catalogue"}
 */
public record RichClientConfig(
        String jdbcUrl,
        String user,
        String password,
        int maximumPoolSize,
        Duration connectionTimeout,
        Duration statementTimeout,
        int transactionRetryAttempts,
        Duration slowQueryLogThreshold,
        OpenTelemetry openTelemetry,
        String scopeName,
        String scopeVersion,
        String poolName,
        String artifactName) {

    /** The version of this module, used as the {@link #defaults} instrumentation-scope version. */
    private static final String MODULE_VERSION = "1.0.0";

    /**
     * Validates the record's components.
     *
     * @throws NullPointerException if any reference-typed component is null
     * @throws IllegalArgumentException if a numeric or duration component violates its stated bound
     */
    public RichClientConfig {
        Objects.requireNonNull(jdbcUrl, "jdbcUrl");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(password, "password");
        Objects.requireNonNull(connectionTimeout, "connectionTimeout");
        Objects.requireNonNull(statementTimeout, "statementTimeout");
        Objects.requireNonNull(slowQueryLogThreshold, "slowQueryLogThreshold");
        Objects.requireNonNull(openTelemetry, "openTelemetry");
        Objects.requireNonNull(scopeName, "scopeName");
        Objects.requireNonNull(scopeVersion, "scopeVersion");
        Objects.requireNonNull(poolName, "poolName");
        Objects.requireNonNull(artifactName, "artifactName");
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
     * <p>Because this factory has no knowledge of the calling artifact, the instrumentation
     * scope name, scope version, pool name and artifact name are populated with generic,
     * library-level defaults ({@code "io.pgenie.java.rich-client"}, this module's own version,
     * {@code "rich-client-pool"} and {@code "rich-client"} respectively) so that the type remains
     * usable standalone, without a generator. Real generated callers are expected to override all
     * four via {@link #withScopeName}, {@link #withScopeVersion}, {@link #withPoolName} and
     * {@link #withArtifactName} with values that identify their own artifact.</p>
     *
     * @param jdbcUrl the JDBC URL of the PostgreSQL database
     * @param user the database user
     * @param password the database password
     * @return a fully-populated config
     * @throws NullPointerException if any argument is null
     */
    public static RichClientConfig defaults(String jdbcUrl, String user, String password) {
        return new RichClientConfig(
                jdbcUrl,
                user,
                password,
                10,
                Duration.ofSeconds(30),
                Duration.ofSeconds(30),
                3,
                Duration.ofSeconds(1),
                GlobalOpenTelemetry.get(),
                "io.pgenie.java.rich-client",
                MODULE_VERSION,
                "rich-client-pool",
                "rich-client");
    }

    /**
     * Returns a copy of this config with the given JDBC URL.
     *
     * @param jdbcUrl the JDBC URL to apply
     * @return a new {@code RichClientConfig}
     * @throws NullPointerException if {@code jdbcUrl} is null
     */
    public RichClientConfig withJdbcUrl(String jdbcUrl) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given database user.
     *
     * @param user the user to apply
     * @return a new {@code RichClientConfig}
     * @throws NullPointerException if {@code user} is null
     */
    public RichClientConfig withUser(String user) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given database password.
     *
     * @param password the password to apply
     * @return a new {@code RichClientConfig}
     * @throws NullPointerException if {@code password} is null
     */
    public RichClientConfig withPassword(String password) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given maximum pool size.
     *
     * @param maximumPoolSize the maximum pool size to apply; at least 1
     * @return a new {@code RichClientConfig}
     * @throws IllegalArgumentException if {@code maximumPoolSize} is less than 1
     */
    public RichClientConfig withMaximumPoolSize(int maximumPoolSize) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given connection timeout.
     *
     * @param connectionTimeout the connection timeout to apply; must not be negative
     * @return a new {@code RichClientConfig}
     * @throws NullPointerException if {@code connectionTimeout} is null
     * @throws IllegalArgumentException if {@code connectionTimeout} is negative
     */
    public RichClientConfig withConnectionTimeout(Duration connectionTimeout) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given statement timeout.
     *
     * @param statementTimeout the statement timeout to apply; zero means no timeout; must not be negative
     * @return a new {@code RichClientConfig}
     * @throws NullPointerException if {@code statementTimeout} is null
     * @throws IllegalArgumentException if {@code statementTimeout} is negative
     */
    public RichClientConfig withStatementTimeout(Duration statementTimeout) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given transaction retry attempts.
     *
     * @param transactionRetryAttempts the number of retry attempts to apply; at least 1
     * @return a new {@code RichClientConfig}
     * @throws IllegalArgumentException if {@code transactionRetryAttempts} is less than 1
     */
    public RichClientConfig withTransactionRetryAttempts(int transactionRetryAttempts) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given slow-query-log threshold.
     *
     * @param slowQueryLogThreshold the threshold to apply; zero logs every query; must not be negative
     * @return a new {@code RichClientConfig}
     * @throws NullPointerException if {@code slowQueryLogThreshold} is null
     * @throws IllegalArgumentException if {@code slowQueryLogThreshold} is negative
     */
    public RichClientConfig withSlowQueryLogThreshold(Duration slowQueryLogThreshold) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given OpenTelemetry instance.
     *
     * @param openTelemetry the OpenTelemetry instance to apply
     * @return a new {@code RichClientConfig}
     * @throws NullPointerException if {@code openTelemetry} is null
     */
    public RichClientConfig withOpenTelemetry(OpenTelemetry openTelemetry) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given instrumentation-scope name.
     *
     * @param scopeName the scope name to apply
     * @return a new {@code RichClientConfig}
     * @throws NullPointerException if {@code scopeName} is null
     */
    public RichClientConfig withScopeName(String scopeName) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given instrumentation-scope version.
     *
     * @param scopeVersion the scope version to apply
     * @return a new {@code RichClientConfig}
     * @throws NullPointerException if {@code scopeVersion} is null
     */
    public RichClientConfig withScopeVersion(String scopeVersion) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given pool name.
     *
     * @param poolName the pool name to apply
     * @return a new {@code RichClientConfig}
     * @throws NullPointerException if {@code poolName} is null
     */
    public RichClientConfig withPoolName(String poolName) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /**
     * Returns a copy of this config with the given artifact name.
     *
     * @param artifactName the artifact name to apply
     * @return a new {@code RichClientConfig}
     * @throws NullPointerException if {@code artifactName} is null
     */
    public RichClientConfig withArtifactName(String artifactName) {
        return new RichClientConfig(jdbcUrl, user, password, maximumPoolSize, connectionTimeout,
                statementTimeout, transactionRetryAttempts, slowQueryLogThreshold, openTelemetry,
                scopeName, scopeVersion, poolName, artifactName);
    }

    /** Redacts {@link #password()} so it can't leak into logs or exception messages. */
    @Override
    public String toString() {
        return "RichClientConfig["
                + "jdbcUrl=" + jdbcUrl
                + ", user=" + user
                + ", password=***"
                + ", maximumPoolSize=" + maximumPoolSize
                + ", connectionTimeout=" + connectionTimeout
                + ", statementTimeout=" + statementTimeout
                + ", transactionRetryAttempts=" + transactionRetryAttempts
                + ", slowQueryLogThreshold=" + slowQueryLogThreshold
                + ", openTelemetry=" + openTelemetry
                + ", scopeName=" + scopeName
                + ", scopeVersion=" + scopeVersion
                + ", poolName=" + poolName
                + ", artifactName=" + artifactName
                + ']';
    }
}
