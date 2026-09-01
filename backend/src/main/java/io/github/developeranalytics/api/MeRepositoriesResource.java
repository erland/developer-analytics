package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.*;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.util.*;

@Path("/api/me/repositories")
@Produces(MediaType.APPLICATION_JSON)
public class MeRepositoriesResource {
    @Inject CurrentUserService currentUserService;
    @Inject SourceRepositoryRepository repositories;

    @GET
    public List<RepositorySummary> list(@CookieParam(AuthenticationService.SESSION_COOKIE) String token) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        return repositories.findAllForUser(current.user().getId()).stream().map(RepositorySummary::from).toList();
    }

    @GET @Path("/{repositoryId}")
    public RepositorySummary get(@CookieParam(AuthenticationService.SESSION_COOKIE) String token,
                                 @PathParam("repositoryId") UUID repositoryId) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        return repositories.findByIdForUser(repositoryId, current.user().getId()).map(RepositorySummary::from)
                .orElseThrow(NotFoundException::new);
    }

    public record RepositorySummary(UUID id, String provider, String externalRepositoryId, String name,
            String fullName, String htmlUrl, String visibility, String ownershipRelation, boolean includedInAnalysis,
            String syncStatus, OffsetDateTime firstActivityAt, OffsetDateTime lastActivityAt,
            Integer contributorCount, Integer humanContributorCount, Integer botContributorCount,
            Integer userCommitCount, Integer repositoryCommitCount, Long userAdditions, Long userDeletions) {
        static RepositorySummary from(SourceRepository r) {
            return new RepositorySummary(r.getId(), r.getProvider(), r.getExternalRepositoryId(), r.getName(),
                    r.getFullName(), r.getHtmlUrl(), r.getVisibility().name(), r.getOwnershipRelation().name(),
                    r.isIncludedInAnalysis(), r.getSyncStatus().name(), null, r.getLastActivityAt(),
                    r.getContributorCount(), r.getHumanContributorCount(), r.getBotContributorCount(),
                    r.getUserCommitCount(), r.getRepositoryCommitCount(), r.getUserAdditions(), r.getUserDeletions());
        }
    }
}
