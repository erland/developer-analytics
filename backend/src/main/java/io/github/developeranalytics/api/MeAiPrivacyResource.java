package io.github.developeranalytics.api;

import io.github.developeranalytics.ai.AiPrivacyPolicy;
import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/me/ai/privacy")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MeAiPrivacyResource {

    @Inject
    CurrentUserService currentUserService;

    @GET
    public Settings get(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(token);
        return Settings.from(
                current.user().getAiPrivacyPolicy()
        );
    }

    @PUT
    @Transactional
    public Settings update(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            UpdateRequest request
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(token);

        if (request == null || request.policy() == null) {
            throw new BadRequestException(
                    "AI privacy policy must be explicitly supplied"
            );
        }

        current.user().setAiPrivacyPolicy(
                request.policy()
        );

        return Settings.from(
                current.user().getAiPrivacyPolicy()
        );
    }

    public record UpdateRequest(
            AiPrivacyPolicy policy
    ) {}

    public record Settings(
            AiPrivacyPolicy policy,
            boolean privateMetadataAllowed,
            boolean privateContentAllowed
    ) {
        static Settings from(AiPrivacyPolicy policy) {
            return new Settings(
                    policy,
                    policy.allowsPrivateMetadata(),
                    false
            );
        }
    }
}
