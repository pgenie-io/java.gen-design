package io.pgenie.artifacts.myspace.musiccatalogue;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class HealthCheckIT extends AbstractDatabaseIT {

    @Test

    void healthySessionReturnsTrue() throws Exception {
        assertTrue(session.healthCheck());
    }

    @Test

    void closedSessionHealthCheckReturnsFalse() throws Exception {
        session.close();
        assertFalse(session.healthCheck());
    }
}
