package io.pgenie.artifacts.myspace.musiccatalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.codemine.java.postgresql.jdbc.IsolationLevel;
import io.codemine.java.postgresql.jdbc.TransactionSettings;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the pure, package-private static helpers of {@link MusicCatalogueSession}:
 * default transaction settings, isolation-level attribute formatting, retry-count arithmetic and
 * JDBC URL redaction. Each takes its inputs as plain parameters so it can be tested without
 * standing up the session's HikariCP pool.
 */
class MusicCatalogueSessionTest {

    @Test
    void defaultTransactionSettingsUsesSerializableReadWrite() {
        var config = MusicCatalogueConfig.builder()
                .jdbcUrl("jdbc:postgresql://localhost/test")
                .user("user")
                .password("pw")
                .transactionRetryAttempts(5)
                .build();

        TransactionSettings settings = MusicCatalogueSession.defaultTransactionSettings(config);

        assertEquals(IsolationLevel.SERIALIZABLE, settings.isolationLevel());
        assertFalse(settings.readOnly());
        assertEquals(5, settings.maxAttempts());
    }

    @Test
    void defaultTransactionSettingsClampsNonPositiveRetryAttemptsToOne() {
        var config = MusicCatalogueConfig.builder()
                .jdbcUrl("jdbc:postgresql://localhost/test")
                .user("user")
                .password("pw")
                .transactionRetryAttempts(0)
                .build();

        TransactionSettings settings = MusicCatalogueSession.defaultTransactionSettings(config);

        assertEquals(1, settings.maxAttempts());
    }

    @Test
    void isolationLevelAttributeReturnsIsolationLevelName() {
        TransactionSettings settings = new TransactionSettings(IsolationLevel.REPEATABLE_READ, true, 3);

        assertEquals("REPEATABLE_READ", MusicCatalogueSession.isolationLevelAttribute(settings));
    }

    @Test
    void retryCountCountsEachRollbackWhenTransactionEventuallyCommits() {
        assertEquals(0, MusicCatalogueSession.retryCount(true, 0));
        assertEquals(2, MusicCatalogueSession.retryCount(true, 2));
    }

    @Test
    void retryCountExcludesFinalRollbackWhenTransactionNeverCommits() {
        assertEquals(0, MusicCatalogueSession.retryCount(false, 0));
        assertEquals(0, MusicCatalogueSession.retryCount(false, 1));
        assertEquals(2, MusicCatalogueSession.retryCount(false, 3));
    }

    @Test
    void redactUrlMasksPasswordUpToNextAmpersand() {
        String redacted = MusicCatalogueSession.redactUrl(
                "jdbc:postgresql://localhost/test?password=secret&sslmode=require");

        assertEquals("jdbc:postgresql://localhost/test?password=***&sslmode=require", redacted);
    }

    @Test
    void redactUrlMasksPasswordToEndOfStringWhenNoTrailingParam() {
        String redacted = MusicCatalogueSession.redactUrl("jdbc:postgresql://localhost/test?password=secret");

        assertEquals("jdbc:postgresql://localhost/test?password=***", redacted);
    }

    @Test
    void redactUrlIsCaseInsensitiveForThePasswordKey() {
        String redacted = MusicCatalogueSession.redactUrl("jdbc:postgresql://localhost/test?PASSWORD=secret");

        assertEquals("jdbc:postgresql://localhost/test?PASSWORD=***", redacted);
    }

    @Test
    void redactUrlReturnsUrlUnchangedWhenNoPasswordPresent() {
        String url = "jdbc:postgresql://localhost/test?sslmode=require";

        assertEquals(url, MusicCatalogueSession.redactUrl(url));
    }

    @Test
    void redactUrlReturnsNullForNullInput() {
        assertNull(MusicCatalogueSession.redactUrl(null));
    }
}
