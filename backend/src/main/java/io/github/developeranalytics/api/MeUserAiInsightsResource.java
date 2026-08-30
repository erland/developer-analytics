package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.insight.UserAiInsight;
import io.github.developeranalytics.persistence.insight.UserAiInsightRepository;
import io.github.developeranalytics.service.insight.UserAiInsightService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;

@Path("/api/me/ai/insights")
@Produces(MediaType.APPLICATION_JSON)
public class MeUserAiInsightsResource {

    @Inject CurrentUserService currentUserService;
    @Inject UserAiInsightRepository insights;
    @Inject UserAiInsightService service;

    @GET
    public ResponseModel latest(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(token);

        return insights.latest(current.user().getId())
                .map(value -> ResponseModel.from("REUSED", value))
                .orElse(null);
    }

    @POST
    public ResponseModel generate(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(token);

        UserAiInsightService.Result result =
                service.generate(current.user());

        if (result.insight() == null) {
            return new ResponseModel(
                    result.status().name(),
                    true,
                    List.of(),
                    "",
                    "",
                    "",
                    "",
                    UserAiInsightService.ANALYSIS_VERSION,
                    null,
                    null,
                    "PUBLIC_ONLY",
                    null
            );
        }

        return ResponseModel.from(
                result.status().name(),
                result.insight()
        );
    }

    public record Role(
            String role,
            double confidence,
            String rationale
    ) {}

    public record ResponseModel(
            String status,
            boolean aiGenerated,
            List<Role> likelyRoles,
            String technicalFocus,
            String breadthDepthObservation,
            String technologyEvolutionSummary,
            String openSourceEngagementSummary,
            String analysisVersion,
            String providerId,
            String modelId,
            String privacyProvenance,
            OffsetDateTime createdAt
    ) {
        static ResponseModel from(
                String status,
                UserAiInsight value
        ) {
            return new ResponseModel(
                    status,
                    true,
                    value.getLikelyRoles().stream()
                            .map(role -> new Role(
                                    role.role(),
                                    role.confidence(),
                                    role.rationale()
                            ))
                            .toList(),
                    value.getTechnicalFocus(),
                    value.getBreadthDepthObservation(),
                    value.getTechnologyEvolutionSummary(),
                    value.getOpenSourceEngagementSummary(),
                    value.getAnalysisVersion(),
                    value.getProviderId(),
                    value.getModelId(),
                    value.getPrivacyProvenance().name(),
                    value.getCreatedAt()
            );
        }
    }
}
