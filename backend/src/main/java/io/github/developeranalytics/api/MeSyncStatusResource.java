package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.model.RepositorySyncRun;
import io.github.developeranalytics.persistence.repository.RepositorySyncRunRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/me/sync-runs")
@Produces(MediaType.APPLICATION_JSON)
public class MeSyncStatusResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    RepositorySyncRunRepository syncRuns;

    @GET
    public List<SyncRunSummary> recent(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        return syncRuns.findRecentForUser(current.user().getId()).stream()
                .map(SyncRunSummary::from)
                .toList();
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
            String provider,
            String status,
            int repositoriesSeen,
            int repositoriesCreated,
            int repositoriesUpdated,
            int pagesProcessed,
            Integer rateLimitRemaining,
            OffsetDateTime rateLimitResetAt,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            String lastError
    ) {
        static SyncRunSummary from(RepositorySyncRun run) {
            return new SyncRunSummary(
                    run.getId(),
                    run.getProvider(),
                    run.getStatus().name(),
                    run.getRepositoriesSeen(),
                    run.getRepositoriesCreated(),
                    run.getRepositoriesUpdated(),
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
