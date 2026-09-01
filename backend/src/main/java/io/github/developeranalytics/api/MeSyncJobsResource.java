package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.job.BackgroundJob;
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
        List<BackgroundJob> recent = jobs.findRecentForUser(current.user().getId(), limit);
        return overview(current.user().getId(), recent);
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
        long queued = recent.stream().filter(j -> j.getStatus().name().equals("QUEUED")).count();
        long waiting = recent.stream().filter(j -> j.getStatus().name().equals("WAITING")).count();
        long running = recent.stream().filter(j -> j.getStatus().name().equals("RUNNING")).count();
        long completed = recent.stream().filter(j -> j.getStatus().name().equals("COMPLETED")).count();
        long failed = recent.stream().filter(j -> j.getStatus().name().equals("FAILED")).count();
        List<JobSummary> active = recent.stream()
                .filter(j -> Set.of("QUEUED", "WAITING", "RUNNING").contains(j.getStatus().name()))
                .limit(25)
                .map(j -> summary(userId, j))
                .toList();
        return new JobOverview(queued, waiting, running, completed, failed, active);
    }

    private JobSummary summary(UUID userId, BackgroundJob job) {
        UUID repositoryId = repositoryId(job);
        String repositoryName = repositoryId == null ? null : repositories
                .findByIdForUser(repositoryId, userId)
                .map(r -> r.getName())
                .orElse(null);
        return new JobSummary(
                job.getId(), job.getJobType(), job.getStatus().name(), repositoryId,
                repositoryName, job.getAttemptCount(), job.getMaxAttempts(),
                job.getProgressPercent(), job.getLastError(), job.getCreatedAt(),
                job.getNextExecutionAt(), job.getLockedAt(), job.getCompletedAt()
        );
    }

    private UUID repositoryId(BackgroundJob job) {
        Object value = job.getPayload() == null ? null : job.getPayload().get("repositoryId");
        if (value == null) return null;
        try { return UUID.fromString(String.valueOf(value)); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    public record JobOverview(
            long queued, long waiting, long running, long completed, long failed,
            List<JobSummary> activeJobs
    ) {}

    public record JobSummary(
            UUID id, String jobType, String status, UUID repositoryId,
            String repositoryName, int attemptCount, int maxAttempts,
            Integer progressPercent, String lastError, OffsetDateTime createdAt,
            OffsetDateTime nextExecutionAt, OffsetDateTime startedAt,
            OffsetDateTime completedAt
    ) {}
}
