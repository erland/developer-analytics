package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.technology.RepositoryTechnologyEvidence;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.persistence.technology.RepositoryTechnologyEvidenceRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/me/repositories/{repositoryId}/technology-evidence")
@Produces(MediaType.APPLICATION_JSON)
public class MeRepositoryTechnologyEvidenceResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    SourceRepositoryRepository repositories;

    @Inject
    RepositoryTechnologyEvidenceRepository evidence;

    @GET
    public List<Entry> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("repositoryId") UUID repositoryId
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);

        repositories.findByIdForUser(repositoryId, current.user().getId())
                .orElseThrow(NotFoundException::new);

        return evidence.findForRepository(
                        current.user().getId(),
                        repositoryId
                ).stream()
                .map(Entry::from)
                .toList();
    }

    public record Entry(
            String technologyKey,
            String technologyName,
            String evidenceType,
            String strength,
            String sourceValue,
            Long measuredValue,
            OffsetDateTime observedAt
    ) {
        static Entry from(RepositoryTechnologyEvidence evidence) {
            return new Entry(
                    evidence.getTechnology().getTechnologyKey(),
                    evidence.getTechnology().getDisplayName(),
                    evidence.getEvidenceType().name(),
                    evidence.getStrength().name(),
                    evidence.getSourceValue(),
                    evidence.getMeasuredValue(),
                    evidence.getObservedAt()
            );
        }
    }
}
