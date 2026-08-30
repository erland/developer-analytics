package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.project.AiProjectClassification;
import io.github.developeranalytics.persistence.project.AiProjectClassificationRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.service.project.AiProjectClassificationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/me/projects/{repositoryId}/ai-classification")
@Produces(MediaType.APPLICATION_JSON)
public class MeProjectAiClassificationResource {

    @Inject CurrentUserService currentUserService;
    @Inject SourceRepositoryRepository repositories;
    @Inject AiProjectClassificationRepository aiClassifications;
    @Inject AiProjectClassificationService service;

    @GET
    public ResponseModel latest(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            @PathParam("repositoryId") UUID repositoryId
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        repositories.findByIdForUser(repositoryId, current.user().getId())
                .orElseThrow(NotFoundException::new);

        return aiClassifications.latest(repositoryId)
                .map(value -> ResponseModel.from("REUSED", value))
                .orElse(null);
    }

    @POST
    public ResponseModel classify(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            @PathParam("repositoryId") UUID repositoryId
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        var repository = repositories
                .findByIdForUser(repositoryId, current.user().getId())
                .orElseThrow(NotFoundException::new);

        AiProjectClassificationService.Result result =
                service.classify(repository);

        if (result.classification() == null) {
            return new ResponseModel(
                    result.status().name(),
                    List.of(),
                    0,
                    null,
                    AiProjectClassificationService.ANALYSIS_VERSION,
                    null,
                    null,
                    null,
                    null
            );
        }

        return ResponseModel.from(
                result.status().name(),
                result.classification()
        );
    }

    public record ResponseModel(
            String status,
            List<String> classifications,
            double confidence,
            String explanation,
            String analysisVersion,
            String providerId,
            String modelId,
            String privacyProvenance,
            OffsetDateTime createdAt
    ) {
        static ResponseModel from(
                String status,
                AiProjectClassification value
        ) {
            return new ResponseModel(
                    status,
                    value.getClassifications(),
                    value.getConfidence(),
                    value.getExplanation(),
                    value.getAnalysisVersion(),
                    value.getProviderId(),
                    value.getModelId(),
                    value.getPrivacyProvenance().name(),
                    value.getCreatedAt()
            );
        }
    }
}
