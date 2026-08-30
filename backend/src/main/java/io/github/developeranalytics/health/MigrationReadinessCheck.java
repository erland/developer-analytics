package io.github.developeranalytics.health;

import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
public class MigrationReadinessCheck implements HealthCheck {

    @Inject EntityManager entityManager;

    @Override
    public HealthCheckResponse call() {
        try {
            Number failed = (Number) entityManager.createNativeQuery(
                    "SELECT count(*) FROM flyway_schema_history " +
                    "WHERE success = false"
            ).getSingleResult();

            Number latest = (Number) entityManager.createNativeQuery(
                    "SELECT COALESCE(MAX(installed_rank), 0) " +
                    "FROM flyway_schema_history WHERE success = true"
            ).getSingleResult();

            if (failed.longValue() > 0) {
                return HealthCheckResponse.named("database-migrations")
                        .down()
                        .withData("failedMigrations", failed.longValue())
                        .withData("latestInstalledRank", latest.longValue())
                        .build();
            }

            return HealthCheckResponse.named("database-migrations")
                    .up()
                    .withData("latestInstalledRank", latest.longValue())
                    .build();
        } catch (RuntimeException failure) {
            return HealthCheckResponse.named("database-migrations")
                    .down()
                    .withData("errorType",
                            failure.getClass().getSimpleName())
                    .build();
        }
    }
}
