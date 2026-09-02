package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.RepositorySyncStatus;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.*;

@Path("/api/me/sync-jobs")
@Produces(MediaType.APPLICATION_JSON)
public class MeSyncJobsResource {
    @Inject CurrentUserService currentUserService;
    @Inject BackgroundJobRepository jobs;
    @Inject SourceRepositoryRepository repositories;

    @GET
    public JobOverview recent(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @QueryParam("limit") @DefaultValue("100") int limit
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        UUID userId = current.user().getId();
        List<BackgroundJob> recent = jobs.findRecentForUser(userId, limit);
        return overview(userId, recent);
    }

    @GET
    @Path("/errors")
    public List<JobSummary> errors(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @QueryParam("limit") @DefaultValue("50") int limit
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        return jobs.findRecentErrorsForUser(current.user().getId(), limit).stream()
                .map(job -> summary(current.user().getId(), job))
                .toList();
    }

    private JobOverview overview(UUID userId, List<BackgroundJob> recent) {
        // The status cards describe repository analysis progress, not the truncated
        // recent-job page. This keeps the total aligned with the repository inventory.
        List<SourceRepository> repositoriesForAnalysis = repositories.findAllForUser(userId).stream()
                .filter(SourceRepository::isIncludedInAnalysis)
                .toList();
        long queued = repositoriesForAnalysis.stream()
                .filter(r -> r.getSyncStatus() == RepositorySyncStatus.QUEUED).count();
        long waiting = repositoriesForAnalysis.stream()
                .filter(r -> r.getSyncStatus() == RepositorySyncStatus.NOT_SYNCED).count();
        long running = repositoriesForAnalysis.stream()
                .filter(r -> r.getSyncStatus() == RepositorySyncStatus.SYNCING).count();
        long completed = repositoriesForAnalysis.stream()
                .filter(r -> r.getSyncStatus() == RepositorySyncStatus.SYNCED).count();
        long failed = repositoriesForAnalysis.stream()
                .filter(r -> r.getSyncStatus() == RepositorySyncStatus.FAILED
                        || r.getSyncStatus() == RepositorySyncStatus.ACCESS_REVOKED).count();

        List<JobSummary> active = recent.stream()
                .filter(j -> Set.of("QUEUED", "WAITING", "RUNNING").contains(j.getStatus().name()))
                .limit(25)
                .map(j -> summary(userId, j))
                .toList();
        return new JobOverview(queued, waiting, running, completed, failed,
                repositoriesForAnalysis.size(), active);
    }

    private JobSummary summary(UUID userId, BackgroundJob job) {
        UUID repositoryId = repositoryId(job);
        String repositoryName = repositoryId == null ? null : repositories
                .findByIdForUser(repositoryId, userId)
                .map(SourceRepository::getName)
                .orElse(null);
        return new JobSummary(
                job.getId(), job.getJobType(), job.getStatus().name(), repositoryId,
                repositoryName, job.getAttemptCount(), job.getMaxAttempts(),
                job.getProgressPercent(), job.getLastError(), job.getCreatedAt(),
                job.getNextExecutionAt(), job.getLockedAt(), job.getCompletedAt());
    }

    private UUID repositoryId(BackgroundJob job) {
        Object value = job.getPayload() == null ? null : job.getPayload().get("repositoryId");
        if (value == null) return null;
        try { return UUID.fromString(String.valueOf(value)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public record JobOverview(
            long queued, long waiting, long running, long completed, long failed,
            int totalRepositories, List<JobSummary> activeJobs
    ) {}

    public record JobSummary(
            UUID id, String jobType, String status, UUID repositoryId,
            String repositoryName, int attemptCount, int maxAttempts,
            Integer progressPercent, String lastError, OffsetDateTime createdAt,
            OffsetDateTime nextExecutionAt, OffsetDateTime startedAt,
            OffsetDateTime completedAt
    ) {}
}
