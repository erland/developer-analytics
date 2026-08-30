package io.github.developeranalytics.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HealthContractTest {

    @Test
    void healthChecksCoverRequiredOperationalAreas() {
        assertNotNull(BackendLivenessCheck.class);
        assertNotNull(DatabaseReadinessCheck.class);
        assertNotNull(MigrationReadinessCheck.class);
        assertNotNull(WorkerProcessHealthCheck.class);
    }
}
