package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.correction.UserAnalysisCorrection;
import io.github.developeranalytics.persistence.correction.UserAnalysisCorrectionRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.service.correction.UserCorrectionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/me/corrections")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MeAnalysisCorrectionsResource {

    @Inject CurrentUserService currentUserService;
    @Inject SourceRepositoryRepository repositories;
    @Inject UserAnalysisCorrectionRepository correctionRepository;
    @Inject UserCorrectionService corrections;

    @GET
    public List<Item> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        return correctionRepository.findForUser(current.user().getId())
                .stream()
                .map(Item::from)
                .toList();
    }

    @PUT
    @Path("/projects/{repositoryId}/categories/{categoryKey}")
    public void rejectCategory(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            @PathParam("repositoryId") UUID repositoryId,
            @PathParam("categoryKey") String categoryKey,
            Toggle toggle
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        var repository = repositories.findByIdForUser(
                repositoryId,
                current.user().getId()
        ).orElseThrow(NotFoundException::new);

        corrections.set(
                current.user(),
                repository,
                UserAnalysisCorrection.Type.PROJECT_CATEGORY_REJECTED,
                categoryKey,
                requireToggle(toggle)
        );
    }

    @PUT
    @Path("/technologies/{technologyKey}")
    public void suppressTechnology(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            @PathParam("technologyKey") String technologyKey,
            Toggle toggle
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);

        corrections.set(
                current.user(),
                null,
                UserAnalysisCorrection.Type.TECHNOLOGY_INFERENCE_SUPPRESSED,
                technologyKey,
                requireToggle(toggle)
        );
    }

    @PUT
    @Path("/projects/{repositoryId}/ai-profile")
    public void excludeProjectFromAiProfile(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            @PathParam("repositoryId") UUID repositoryId,
            Toggle toggle
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        var repository = repositories.findByIdForUser(
                repositoryId,
                current.user().getId()
        ).orElseThrow(NotFoundException::new);

        corrections.set(
                current.user(),
                repository,
                UserAnalysisCorrection.Type.PROJECT_EXCLUDED_FROM_AI_PROFILE,
                null,
                requireToggle(toggle)
        );
    }

    private boolean requireToggle(Toggle toggle) {
        if (toggle == null || toggle.enabled() == null) {
            throw new BadRequestException(
                    "Correction enabled state must be explicitly supplied"
            );
        }
        return toggle.enabled();
    }

    public record Toggle(Boolean enabled) {}

    public record Item(
            UUID id,
            String type,
            UUID repositoryId,
            String repositoryName,
            String correctionKey,
            OffsetDateTime createdAt
    ) {
        static Item from(UserAnalysisCorrection correction) {
            return new Item(
                    correction.getId(),
                    correction.getType().name(),
                    correction.getRepository() == null
                            ? null
                            : correction.getRepository().getId(),
                    correction.getRepository() == null
                            ? null
                            : correction.getRepository().getName(),
                    correction.getCorrectionKey(),
                    correction.getCreatedAt()
            );
        }
    }
}
