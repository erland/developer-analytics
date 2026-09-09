package io.github.developeranalytics.service.sync;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.ContributionSyncMode;
import io.github.developeranalytics.domain.model.ContributionSyncRun;
import io.github.developeranalytics.domain.model.ProviderSyncRun;
import io.github.developeranalytics.persistence.repository.ContributionSyncRunRepository;
import io.github.developeranalytics.persistence.repository.ProviderSyncRunRepository;
import io.github.developeranalytics.worker.GitHubContributionDiscoveryJobHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProviderSyncRunService {
    public static final String PAYLOAD_KEY = "providerSyncRunId";

    @Inject ProviderSyncRunRepository runs;
    @Inject ContributionSyncRunRepository contributionRuns;
    @Inject EntityManager em;

    @Transactional
    public ProviderSyncRun start(AppUser user, String provider) {
        ProviderSyncRun run = new ProviderSyncRun(user, provider, OffsetDateTime.now(ZoneOffset.UTC));
        runs.persist(run);
        em.flush();
        return run;
    }

    @Transactional
    public void plan(UUID runId, int repositoriesPlanned) {
        ProviderSyncRun run = runs.findByIdForUpdate(runId).orElseThrow();
        run.setRepositoriesPlanned(repositoriesPlanned);
        if (repositoriesPlanned == 0) run.complete(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public void observeMode(UUID runId, ContributionSyncMode mode) {
        if (runId == null) return;
        runs.findByIdForUpdate(runId).ifPresent(run -> run.observeSyncMode(mode));
    }

    @Transactional
    public void pause(UUID runId, OffsetDateTime now) {
        if (runId == null) return;
        runs.findByIdForUpdate(runId).ifPresent(run -> run.pause(now));
    }

    @Transactional
    public void resume(UUID runId, OffsetDateTime now) {
        if (runId == null) return;
        runs.findByIdForUpdate(runId).ifPresent(run -> run.resume(now));
    }

    @Transactional
    public void refreshCompletion(UUID runId, OffsetDateTime now) {
        if (runId == null) return;
        ProviderSyncRun run = runs.findByIdForUpdate(runId).orElse(null);
        if (run == null || run.getStatus() == ProviderSyncRun.Status.COMPLETED) return;
        JobCounts counts = jobCounts(runId);
        if (counts.total() >= run.getRepositoriesPlanned() && counts.active() == 0) run.complete(now);
    }

    public Optional<ProviderSyncRun> findForUser(UUID runId, UUID userId) {
        return runs.findByIdForUser(runId, userId);
    }

    public List<ProviderSyncRun> recentForUser(UUID userId) { return runs.findRecentForUser(userId); }

    public Summary summarize(ProviderSyncRun run) {
        JobCounts jobs = jobCounts(run.getId());
        List<ContributionSyncRun> segments = contributionRuns.findForProviderSyncRun(run.getId());
        int seen = 0, created = 0, updated = 0, apiRequests = 0;
        Map<String,Integer> endpoints = new LinkedHashMap<>();
        for (ContributionSyncRun segment : segments) {
            seen += segment.getContributionsSeen();
            created += segment.getContributionsCreated();
            updated += segment.getContributionsUpdated();
            apiRequests += segment.getApiRequestCount();
            segment.getApiRequestsByEndpoint().forEach((key, value) -> endpoints.merge(key, value, Integer::sum));
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        long pausedSeconds = run.getPausedDurationSeconds();
        if (run.getPauseStartedAt() != null) pausedSeconds += Math.max(0, Duration.between(run.getPauseStartedAt(), now).getSeconds());
        OffsetDateTime end = run.getCompletedAt() == null ? now : run.getCompletedAt();
        long elapsedSeconds = Math.max(0, Duration.between(run.getStartedAt(), end).getSeconds());
        return new Summary(run.getId(), run.getProvider(), run.getSyncMode(), run.getStatus(),
                run.getRepositoriesPlanned(), jobs.completed(), jobs.failed(), jobs.active(),
                seen, created, updated, apiRequests, Map.copyOf(endpoints),
                run.getRateLimitPauseCount(), pausedSeconds, elapsedSeconds,
                run.getStartedAt(), run.getCompletedAt());
    }

    public UUID payloadRunId(io.github.developeranalytics.domain.job.BackgroundJob job) {
        if (job == null || job.getPayload() == null) return null;
        Object value = job.getPayload().get(PAYLOAD_KEY);
        if (value == null) return null;
        try { return UUID.fromString(value.toString()); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private JobCounts jobCounts(UUID runId) {
        Object[] row = (Object[]) em.createNativeQuery(
                "SELECT count(*), " +
                "count(*) FILTER (WHERE status='COMPLETED'), " +
                "count(*) FILTER (WHERE status IN ('FAILED','CANCELLED')), " +
                "count(*) FILTER (WHERE status IN ('QUEUED','WAITING','PAUSED_RATE_LIMIT','RUNNING')) " +
                "FROM background_job WHERE job_type=:jobType AND payload->>'providerSyncRunId'=:runId")
                .setParameter("jobType", GitHubContributionDiscoveryJobHandler.JOB_TYPE)
                .setParameter("runId", runId.toString())
                .getSingleResult();
        return new JobCounts(number(row[0]), number(row[1]), number(row[2]), number(row[3]));
    }

    private int number(Object value) { return value == null ? 0 : ((Number) value).intValue(); }

    private record JobCounts(int total, int completed, int failed, int active) {}

    public record Summary(UUID id, String provider, ContributionSyncMode syncMode, ProviderSyncRun.Status status,
                          int repositoriesPlanned, int repositoriesCompleted, int repositoriesFailed, int repositoriesActive,
                          int contributionsSeen, int contributionsCreated, int contributionsUpdated,
                          int apiRequestCount, Map<String,Integer> apiRequestsByEndpoint,
                          int rateLimitPauseCount, long pausedDurationSeconds, long elapsedDurationSeconds,
                          OffsetDateTime startedAt, OffsetDateTime completedAt) {}
}
