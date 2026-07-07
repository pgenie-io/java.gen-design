package io.pgenie.artifacts.myspace.musiccatalogue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HealthCheckIT extends AbstractDatabaseIT {

    @Test
    void healthySessionReturnsTrue() {
        assertTrue(session.healthCheck());
    }

    @Test
    void closedSessionThrowsOnHealthCheck() {
        session.close();
        assertThrows(IllegalStateException.class, () -> session.healthCheck());
    }
}
