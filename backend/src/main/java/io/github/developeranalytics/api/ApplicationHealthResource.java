package io.github.developeranalytics.api;

import io.github.developeranalytics.ai.AiAnalysisGateway;
import io.github.developeranalytics.worker.WorkerHeartbeatService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Path("/api/health/application")
@Produces(MediaType.APPLICATION_JSON)
public class ApplicationHealthResource {

    @Inject WorkerHeartbeatService workerHeartbeat;
    @Inject AiAnalysisGateway ai;
    @Inject EntityManager entityManager;

    @GET
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("status", "UP");
        result.put("service", "developer-analytics-backend");
        result.put("timestamp", Instant.now().toString());

        try {
            entityManager.createNativeQuery("SELECT 1").getSingleResult();
            result.put("database", "UP");
        } catch (RuntimeException failure) {
            result.put("database", "DOWN");
            result.put("status", "DEGRADED");
        }

        workerHeartbeat.latestStatus().ifPresentOrElse(
                worker -> result.put(
                        "worker",
                        Map.of(
                                "status",
                                worker.available() ? "UP" : "STALE",
                                "workerId", worker.workerId(),
                                "lastSeenAt",
                                worker.lastSeenAt().toString()
                        )
                ),
                () -> result.put(
                        "worker",
                        Map.of("status", "UNKNOWN")
                )
        );

        AiAnalysisGateway.Availability availability =
                ai.availability();
        result.put(
                "ai",
                Map.of(
                        "provider", availability.providerId(),
                        "model", availability.modelId(),
                        "configured", availability.configured(),
                        "requiredForServiceHealth", false
                )
        );

        return Map.copyOf(result);
    }
}
