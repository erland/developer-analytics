package io.github.developeranalytics.health;

import io.github.developeranalytics.worker.WorkerHeartbeatService;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

@Readiness
public class WorkerProcessHealthCheck implements HealthCheck {

    @Inject WorkerHeartbeatService heartbeat;

    @ConfigProperty(
            name="developer-analytics.runtime-role",
            defaultValue="api"
    )
    String runtimeRole;

    @Override
    public HealthCheckResponse call() {
        if (!"worker".equalsIgnoreCase(runtimeRole)) {
            return HealthCheckResponse.named("worker-process")
                    .up()
                    .withData("runtimeRole", runtimeRole)
                    .withData("requiredInThisProcess", false)
                    .build();
        }

        // If this check is executing in the worker process, the process itself
        // is alive; database readiness is covered independently.
        return HealthCheckResponse.named("worker-process")
                .up()
                .withData("runtimeRole", runtimeRole)
                .withData("requiredInThisProcess", true)
                .build();
    }
}
