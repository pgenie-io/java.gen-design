package io.pgenie.artifacts.myspace.musiccatalogue;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;

import java.time.Duration;
import java.util.Objects;

/**
 * Immutable, builder-style configuration for a {@code MusicCatalogueSession}.
 *
 * <p>The JDBC URL identifies the database host, port and name; credentials are supplied
 * separately so that they can be rotated, stored in secret managers, or overridden without
 * touching the connection string.</p>
 */
public final class MusicCatalogueConfig {

    private final String jdbcUrl;
    private final String user;
    private final String password;
    private final int maximumPoolSize;
    private final Duration connectionTimeout;
    private final Duration statementTimeout;
    private final int transactionRetryAttempts;
    private final Duration slowQueryLogThreshold;
    private final OpenTelemetry openTelemetry;

    private MusicCatalogueConfig(Builder builder) {
        this.jdbcUrl = Objects.requireNonNull(builder.jdbcUrl, "jdbcUrl");
        this.user = Objects.requireNonNull(builder.user, "user");
        this.password = Objects.requireNonNull(builder.password, "password");
        this.maximumPoolSize = builder.maximumPoolSize;
        this.connectionTimeout = builder.connectionTimeout;
        this.statementTimeout = builder.statementTimeout;
        this.transactionRetryAttempts = builder.transactionRetryAttempts;
        this.slowQueryLogThreshold = builder.slowQueryLogThreshold;
        this.openTelemetry = builder.openTelemetry;
    }

    /**
     * Creates a new builder.
     *
     * @return a fresh {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Convenience factory that returns a config with all optional values set to their defaults.
     *
     * @param jdbcUrl  the JDBC URL of the PostgreSQL database
     * @param user     the database user
     * @param password the database password
     * @return a fully-built config
     */
    public static MusicCatalogueConfig defaults(String jdbcUrl, String user, String password) {
        return builder()
            .jdbcUrl(jdbcUrl)
            .user(user)
            .password(password)
            .build();
    }

    /** JDBC URL of the PostgreSQL database. Credentials are supplied separately via {@link #user()} and {@link #password()}. */
    public String jdbcUrl() {
        return jdbcUrl;
    }

    /** Database user. Kept separate from {@link #jdbcUrl()} so that credentials can be rotated independently. */
    public String user() {
        return user;
    }

    /** Database password. Kept separate from {@link #jdbcUrl()} so that credentials can be rotated independently. */
    public String password() {
        return password;
    }

    /** Maximum number of connections maintained in the HikariCP pool. */
    public int maximumPoolSize() {
        return maximumPoolSize;
    }

    /** Maximum time to wait for a connection from the pool before failing. */
    public Duration connectionTimeout() {
        return connectionTimeout;
    }

    /** Maximum time a single statement is allowed to execute before being cancelled. */
    public Duration statementTimeout() {
        return statementTimeout;
    }

    /** Number of times a transaction is retried when a serialization failure or deadlock is detected. */
    public int transactionRetryAttempts() {
        return transactionRetryAttempts;
    }

    /** Queries running longer than this threshold are logged as slow queries. */
    public Duration slowQueryLogThreshold() {
        return slowQueryLogThreshold;
    }

    /** OpenTelemetry instance used for tracing and metrics. */
    public OpenTelemetry openTelemetry() {
        return openTelemetry;
    }

    /**
     * Builder for {@link MusicCatalogueConfig}.
     */
    public static final class Builder {

        private String jdbcUrl;
        private String user;
        private String password;
        private int maximumPoolSize = 10;
        private Duration connectionTimeout = Duration.ofSeconds(30);
        private Duration statementTimeout = Duration.ofSeconds(30);
        private int transactionRetryAttempts = 3;
        private Duration slowQueryLogThreshold = Duration.ofSeconds(1);
        private OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();

        private Builder() {
        }

        /** Sets the JDBC URL of the PostgreSQL database. */
        public Builder jdbcUrl(String jdbcUrl) {
            this.jdbcUrl = jdbcUrl;
            return this;
        }

        /** Sets the database user. */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /** Sets the database password. */
        public Builder password(String password) {
            this.password = password;
            return this;
        }

        /** Sets the maximum pool size. */
        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        /** Sets the maximum wait time for a connection from the pool. */
        public Builder connectionTimeout(Duration connectionTimeout) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        /** Sets the per-statement execution timeout. */
        public Builder statementTimeout(Duration statementTimeout) {
            this.statementTimeout = statementTimeout;
            return this;
        }

        /** Sets the number of transaction retry attempts on serialization failures or deadlocks. */
        public Builder transactionRetryAttempts(int transactionRetryAttempts) {
            this.transactionRetryAttempts = transactionRetryAttempts;
            return this;
        }

        /** Sets the threshold above which a query is considered slow and logged. */
        public Builder slowQueryLogThreshold(Duration slowQueryLogThreshold) {
            this.slowQueryLogThreshold = slowQueryLogThreshold;
            return this;
        }

        /** Sets the OpenTelemetry instance used for tracing and metrics. */
        public Builder openTelemetry(OpenTelemetry openTelemetry) {
            this.openTelemetry = openTelemetry;
            return this;
        }

        /**
         * Builds the configuration.
         *
         * @return a new immutable {@link MusicCatalogueConfig}
         * @throws NullPointerException if any required field is missing
         */
        public MusicCatalogueConfig build() {
            return new MusicCatalogueConfig(this);
        }
    }
}
