package io.github.developeranalytics.api;

import io.github.developeranalytics.ai.AiAnalysisGateway;
import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/me/ai")
@Produces(MediaType.APPLICATION_JSON)
public class MeAiStatusResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    AiAnalysisGateway ai;

    @GET
    @Path("/status")
    public Status status(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token
    ) {
        currentUserService.requireCurrentUser(token);

        AiAnalysisGateway.Availability availability =
                ai.availability();

        return new Status(
                availability.configured(),
                availability.providerId(),
                availability.modelId(),
                currentUserService.requireCurrentUser(token)
                        .user()
                        .getAiPrivacyPolicy()
                        .name(),
                availability.configured()
                        ? "AI-assisted analysis is available."
                        : "AI is not configured. Deterministic analytics remain fully available."
        );
    }

    public record Status(
            boolean configured,
            String providerId,
            String modelId,
            String userPrivacyPolicy,
            String message
    ) {}
}
