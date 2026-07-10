package io.pgenie.artifacts.myspace.musiccatalogue;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.pgenie.java.richclient.RichClientConfig;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class MusicCatalogueConfigTest {

    private static MusicCatalogueConfig defaults() {
        return MusicCatalogueConfig.defaults("jdbc:postgresql://localhost/test", "user", "pw");
    }

    @Test
    void defaultsPopulatesRequiredFields() {
        var config = defaults();

        assertEquals("jdbc:postgresql://localhost/test", config.jdbcUrl());
        assertEquals("user", config.user());
        assertEquals("pw", config.password());
    }

    @Test
    void defaultsPopulatesOptionalFieldsWithDocumentedDefaults() {
        var config = defaults();

        assertEquals(10, config.maximumPoolSize());
        assertEquals(Duration.ofSeconds(30), config.connectionTimeout());
        assertEquals(Duration.ofSeconds(30), config.statementTimeout());
        assertEquals(3, config.transactionRetryAttempts());
        assertEquals(Duration.ofSeconds(1), config.slowQueryLogThreshold());
    }

    @Test
    void requiredFieldsCannotBeNull() {
        assertThrows(NullPointerException.class,
                () -> MusicCatalogueConfig.defaults(null, "user", "pw"));
        assertThrows(NullPointerException.class,
                () -> MusicCatalogueConfig.defaults("jdbc:postgresql://localhost/test", null, "pw"));
        assertThrows(NullPointerException.class,
                () -> MusicCatalogueConfig.defaults("jdbc:postgresql://localhost/test", "user", null));
    }

    @Test
    void withOpenTelemetryRejectsNull() {
        assertThrows(NullPointerException.class, () -> defaults().withOpenTelemetry(null));
    }

    @Test
    void maximumPoolSizeMustBeAtLeastOne() {
        assertThrows(IllegalArgumentException.class, () -> defaults().withMaximumPoolSize(0));
        assertThrows(IllegalArgumentException.class, () -> defaults().withMaximumPoolSize(-1));
        assertDoesNotThrow(() -> defaults().withMaximumPoolSize(1));
    }

    @Test
    void transactionRetryAttemptsMustBeAtLeastOne() {
        assertThrows(IllegalArgumentException.class, () -> defaults().withTransactionRetryAttempts(0));
        assertThrows(IllegalArgumentException.class, () -> defaults().withTransactionRetryAttempts(-1));
        assertDoesNotThrow(() -> defaults().withTransactionRetryAttempts(1));
    }

    @Test
    void statementTimeoutAllowsZeroButRejectsNegative() {
        assertDoesNotThrow(() -> defaults().withStatementTimeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> defaults().withStatementTimeout(Duration.ofSeconds(-1)));
    }

    @Test
    void slowQueryLogThresholdAllowsZeroButRejectsNegative() {
        assertDoesNotThrow(() -> defaults().withSlowQueryLogThreshold(Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> defaults().withSlowQueryLogThreshold(Duration.ofSeconds(-1)));
    }

    @Test
    void connectionTimeoutAllowsZeroButRejectsNegative() {
        assertDoesNotThrow(() -> defaults().withConnectionTimeout(Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> defaults().withConnectionTimeout(Duration.ofSeconds(-1)));
    }

    @Test
    void withersPreserveEveryOtherField() {
        var tweaked = defaults().withMaximumPoolSize(42);
        var expected = new MusicCatalogueConfig(
                "jdbc:postgresql://localhost/test", "user", "pw",
                42, Duration.ofSeconds(30), Duration.ofSeconds(30),
                3, Duration.ofSeconds(1), tweaked.openTelemetry());

        assertEquals(expected, tweaked);
    }

    @Test
    void toStringRedactsPassword() {
        var text = defaults().toString();

        assertFalse(text.contains("pw"), "toString leaked the raw password: " + text);
        assertTrue(text.contains("***"), "toString should contain a redaction marker: " + text);
    }

    @Test
    void toRichClientConfigMapsFieldsAndArtifactIdentity() {
        var config = defaults()
                .withMaximumPoolSize(42)
                .withConnectionTimeout(Duration.ofSeconds(5))
                .withStatementTimeout(Duration.ofSeconds(10))
                .withTransactionRetryAttempts(7)
                .withSlowQueryLogThreshold(Duration.ofMillis(500));

        RichClientConfig rich = config.toRichClientConfig();

        assertEquals(config.jdbcUrl(), rich.jdbcUrl());
        assertEquals(config.user(), rich.user());
        assertEquals(config.password(), rich.password());
        assertEquals(config.maximumPoolSize(), rich.maximumPoolSize());
        assertEquals(config.connectionTimeout(), rich.connectionTimeout());
        assertEquals(config.statementTimeout(), rich.statementTimeout());
        assertEquals(config.transactionRetryAttempts(), rich.transactionRetryAttempts());
        assertEquals(config.slowQueryLogThreshold(), rich.slowQueryLogThreshold());
        assertSame(config.openTelemetry(), rich.openTelemetry());

        assertEquals("io.pgenie.artifacts.myspace.musiccatalogue", rich.scopeName());
        assertEquals("1.0.1", rich.scopeVersion());
        assertEquals("music-catalogue-pool", rich.poolName());
        assertEquals("music-catalogue", rich.artifactName());
    }
}
