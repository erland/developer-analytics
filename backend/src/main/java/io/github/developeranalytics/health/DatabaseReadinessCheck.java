package io.github.developeranalytics.health;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
public class DatabaseReadinessCheck implements HealthCheck {

    @Inject EntityManager entityManager;

    @Override
    public HealthCheckResponse call() {
        try {
            Number value = (Number) entityManager
                    .createNativeQuery("SELECT 1")
                    .getSingleResult();

            return value.intValue() == 1
                    ? HealthCheckResponse.up("database-connectivity")
                    : HealthCheckResponse.down("database-connectivity");
        } catch (RuntimeException failure) {
            return HealthCheckResponse.named("database-connectivity")
                    .down()
                    .withData("errorType",
                            failure.getClass().getSimpleName())
                    .build();
        }
    }
}
