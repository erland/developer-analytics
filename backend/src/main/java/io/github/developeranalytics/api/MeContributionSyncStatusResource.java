package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.model.ContributionSyncRun;
import io.github.developeranalytics.persistence.repository.ContributionSyncRunRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/me/contribution-sync-runs")
@Produces(MediaType.APPLICATION_JSON)
public class MeContributionSyncStatusResource {
    @Inject
    CurrentUserService currentUserService;

    @Inject
    ContributionSyncRunRepository syncRuns;

    @GET
    public List<SyncRunSummary> recent(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @QueryParam("repositoryId") UUID repositoryId
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        List<ContributionSyncRun> runs = repositoryId == null
                ? syncRuns.findRecentForUser(current.user().getId())
                : syncRuns.findRecentForRepository(current.user().getId(), repositoryId);
        return runs.stream().map(SyncRunSummary::from).toList();
    }

    @GET
    @Path("/{id}")
    public SyncRunSummary get(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("id") UUID id
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        return syncRuns.findByIdForUser(id, current.user().getId())
                .map(SyncRunSummary::from)
                .orElseThrow(NotFoundException::new);
    }

    public record SyncRunSummary(
            UUID id,
            UUID repositoryId,
            String repositoryName,
            String provider,
            String status,
            int contributionsSeen,
            int contributionsCreated,
            int contributionsUpdated,
            int pagesProcessed,
            Integer rateLimitRemaining,
            OffsetDateTime rateLimitResetAt,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            String lastError
    ) {
        static SyncRunSummary from(ContributionSyncRun run) {
            return new SyncRunSummary(
                    run.getId(),
                    run.getRepository().getId(),
                    run.getRepository().getName(),
                    run.getProvider(),
                    run.getStatus().name(),
                    run.getContributionsSeen(),
                    run.getContributionsCreated(),
                    run.getContributionsUpdated(),
                    run.getPagesProcessed(),
                    run.getRateLimitRemaining(),
                    run.getRateLimitResetAt(),
                    run.getStartedAt(),
                    run.getCompletedAt(),
                    run.getLastError()
            );
        }
    }
}
