package io.github.developeranalytics.api;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.Instant;
import java.util.Map;

@Path("/api/health/application")
@Produces(MediaType.APPLICATION_JSON)
public class ApplicationHealthResource {

    @GET
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "developer-analytics-backend",
                "timestamp", Instant.now().toString()
        );
    }
}
