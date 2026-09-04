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
    private static final int ANALYSIS_STEPS_PER_REPOSITORY = 4;
    private static final Map<String, Integer> ANALYSIS_STAGE = Map.of(
            "GITHUB_CONTRIBUTION_DISCOVERY", 1,
            "GITHUB_LANGUAGE_EVIDENCE", 2,
            "GITHUB_FILE_MANIFEST_EVIDENCE", 3,
            "DETERMINISTIC_PROJECT_CLASSIFICATION", 4
    );

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
        return overview(userId, jobs.findActiveForUser(userId), limit);
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

    private JobOverview overview(UUID userId, List<BackgroundJob> activeJobs, int requestedLimit) {
        List<SourceRepository> repositoriesForAnalysis = repositories.findAllForUser(userId).stream()
                .filter(SourceRepository::isIncludedInAnalysis)
                .toList();

        Map<UUID, List<BackgroundJob>> activeByRepository = new HashMap<>();
        for (BackgroundJob job : activeJobs) {
            UUID repositoryId = repositoryId(job);
            if (repositoryId != null) {
                activeByRepository.computeIfAbsent(repositoryId, ignored -> new ArrayList<>()).add(job);
            }
        }

        long queued = 0;
        long waiting = 0;
        long running = 0;
        long completed = 0;
        long failed = 0;
        long analysisStepsCompleted = 0;

        for (SourceRepository repository : repositoriesForAnalysis) {
            List<BackgroundJob> repositoryJobs = activeByRepository.getOrDefault(repository.getId(), List.of());
            long activeAnalysisJobs = repositoryJobs.stream()
                    .filter(job -> ANALYSIS_STAGE.containsKey(job.getJobType()))
                    .count();

            if (activeAnalysisJobs > 0) {
                analysisStepsCompleted += Math.max(0, ANALYSIS_STEPS_PER_REPOSITORY - activeAnalysisJobs);
            } else if (!repository.needsAnalysisRefresh()) {
                analysisStepsCompleted += ANALYSIS_STEPS_PER_REPOSITORY;
            }

            if (repositoryJobs.stream().anyMatch(job -> job.getStatus().name().equals("RUNNING"))) {
                running++;
            } else if (repositoryJobs.stream().anyMatch(job -> job.getStatus().name().equals("WAITING"))) {
                waiting++;
            } else if (repositoryJobs.stream().anyMatch(job -> job.getStatus().name().equals("QUEUED"))) {
                queued++;
            } else if (repository.getSyncStatus() == RepositorySyncStatus.FAILED
                    || repository.getSyncStatus() == RepositorySyncStatus.ACCESS_REVOKED) {
                failed++;
            } else if (repository.needsAnalysisRefresh()
                    || repository.getSyncStatus() == RepositorySyncStatus.NOT_SYNCED) {
                waiting++;
            } else if (repository.getSyncStatus() == RepositorySyncStatus.QUEUED) {
                queued++;
            } else if (repository.getSyncStatus() == RepositorySyncStatus.SYNCING) {
                running++;
            } else {
                completed++;
            }
        }

        int displayLimit = Math.max(1, Math.min(requestedLimit, 25));
        List<JobSummary> active = activeJobs.stream()
                .sorted(Comparator
                        .comparing((BackgroundJob job) -> !job.getStatus().name().equals("RUNNING"))
                        .thenComparing(BackgroundJob::getCreatedAt))
                .limit(displayLimit)
                .map(job -> summary(userId, job))
                .toList();

        long analysisStepsTotal = (long) repositoriesForAnalysis.size() * ANALYSIS_STEPS_PER_REPOSITORY;
        return new JobOverview(queued, waiting, running, completed, failed,
                repositoriesForAnalysis.size(), analysisStepsCompleted, analysisStepsTotal, active);
    }

    private JobSummary summary(UUID userId, BackgroundJob job) {
        UUID repositoryId = repositoryId(job);
        String repositoryName = repositoryId == null ? null : repositories
                .findByIdForUser(repositoryId, userId)
                .map(SourceRepository::getName)
                .orElse(null);
        Integer analysisStep = ANALYSIS_STAGE.get(job.getJobType());
        return new JobSummary(
                job.getId(), job.getJobType(), job.getStatus().name(), repositoryId,
                repositoryName, job.getAttemptCount(), job.getMaxAttempts(),
                analysisStep, analysisStep == null ? null : ANALYSIS_STEPS_PER_REPOSITORY,
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
            int totalRepositories, long analysisStepsCompleted, long analysisStepsTotal,
            List<JobSummary> activeJobs
    ) {}

    public record JobSummary(
            UUID id, String jobType, String status, UUID repositoryId,
            String repositoryName, int attemptCount, int maxAttempts,
            Integer analysisStep, Integer analysisStepsTotal,
            Integer progressPercent, String lastError, OffsetDateTime createdAt,
            OffsetDateTime nextExecutionAt, OffsetDateTime startedAt,
            OffsetDateTime completedAt
    ) {}
}
