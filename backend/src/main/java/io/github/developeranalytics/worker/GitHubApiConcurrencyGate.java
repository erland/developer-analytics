package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.OffsetDateTime;
import java.util.Set;

@ApplicationScoped
public class GitHubApiConcurrencyGate {

    private static final Set<String> API_HEAVY_JOB_TYPES = Set.of(
            GitHubRepositoryDiscoveryJobHandler.JOB_TYPE,
            GitHubContributionDiscoveryJobHandler.JOB_TYPE,
            GitHubLanguageEvidenceJobHandler.JOB_TYPE
    );

    @Inject BackgroundJobRepository jobs;

    @ConfigProperty(name = "developer-analytics.github.max-concurrent-api-jobs-per-user", defaultValue = "2")
    int maxConcurrentJobsPerUser;

    @ConfigProperty(name = "developer-analytics.github.concurrency-defer-seconds", defaultValue = "5")
    long deferSeconds;

    public Decision decision(BackgroundJob job, OffsetDateTime now) {
        if (job == null || job.getUser() == null || !isApiHeavyJobType(job.getJobType())) {
            return Decision.permit();
        }

        int limit = Math.max(1, maxConcurrentJobsPerUser);
        long otherRunning = jobs.countRunningJobsForUserByTypesExcept(
                job.getUser().getId(),
                API_HEAVY_JOB_TYPES,
                job.getId()
        );

        if (otherRunning < limit) {
            return Decision.permit();
        }

        OffsetDateTime observedAt = now == null ? OffsetDateTime.now() : now;
        return Decision.blocked(observedAt.plusSeconds(Math.max(1L, deferSeconds)), otherRunning, limit);
    }

    static boolean isApiHeavyJobType(String jobType) {
        return jobType != null && API_HEAVY_JOB_TYPES.contains(jobType);
    }

    public record Decision(boolean allowed, OffsetDateTime retryAt, long running, int limit) {
        static Decision permit() {
            return new Decision(true, null, 0L, 0);
        }

        static Decision blocked(OffsetDateTime retryAt, long running, int limit) {
            return new Decision(false, retryAt, running, limit);
        }
    }
}
