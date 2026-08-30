package io.github.developeranalytics.service.job;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import io.github.developeranalytics.worker.GitHubRepositoryDiscoveryJobHandler;
import io.github.developeranalytics.worker.GitHubContributionDiscoveryJobHandler;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@ApplicationScoped
public class RepositoryDiscoveryJobService {

    @Inject
    BackgroundJobRepository jobs;

    @Transactional
    public BackgroundJob enqueueContributionDiscovery(AppUser user, java.util.UUID repositoryId) {
        String deduplicationKey = "github:contributions:" + repositoryId;

        if (jobs.existsActiveDeduplicatedJob(user.getId(), deduplicationKey)) {
            return null;
        }

        BackgroundJob job = BackgroundJob.queuedDeduplicated(
                user,
                GitHubContributionDiscoveryJobHandler.JOB_TYPE,
                110,
                Map.of("provider", "github", "repositoryId", repositoryId.toString()),
                5,
                OffsetDateTime.now(ZoneOffset.UTC),
                deduplicationKey
        );
        jobs.persist(job);
        return job;
    }

    public BackgroundJob enqueueGitHubDiscovery(AppUser user) {
        BackgroundJob job = BackgroundJob.queued(
                user,
                GitHubRepositoryDiscoveryJobHandler.JOB_TYPE,
                100,
                Map.of("provider", "github"),
                5,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        jobs.persist(job);
        return job;
    }
}
