package io.github.developeranalytics.service.job;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import io.github.developeranalytics.worker.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class RepositoryDiscoveryJobService {

    @Inject
    BackgroundJobRepository jobs;

    @Transactional
    public BackgroundJob enqueueLanguageEvidence(
            AppUser user,
            UUID repositoryId
    ) {
        return enqueueDeduplicated(
                user,
                repositoryId,
                GitHubLanguageEvidenceJobHandler.JOB_TYPE,
                120,
                "github:language-evidence:"
        );
    }

    @Transactional
    public BackgroundJob enqueueFileManifestEvidence(
            AppUser user,
            UUID repositoryId
    ) {
        return enqueueDeduplicated(
                user,
                repositoryId,
                GitHubFileManifestEvidenceJobHandler.JOB_TYPE,
                125,
                "github:file-manifest-evidence:"
        );
    }

    @Transactional
    public BackgroundJob enqueueDeterministicClassification(
            AppUser user,
            UUID repositoryId
    ) {
        return enqueueDeduplicated(
                user,
                repositoryId,
                DeterministicProjectClassificationJobHandler.JOB_TYPE,
                130,
                "project-classification:"
        );
    }

    @Transactional
    public BackgroundJob enqueueContributionDiscovery(
            AppUser user,
            UUID repositoryId
    ) {
        return enqueueDeduplicated(
                user,
                repositoryId,
                GitHubContributionDiscoveryJobHandler.JOB_TYPE,
                110,
                "github:contributions:"
        );
    }

    @Transactional
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

    private BackgroundJob enqueueDeduplicated(
            AppUser user,
            UUID repositoryId,
            String jobType,
            int priority,
            String deduplicationPrefix
    ) {
        String deduplicationKey =
                deduplicationPrefix + repositoryId;

        if (jobs.existsActiveDeduplicatedJob(
                user.getId(),
                deduplicationKey
        )) {
            return null;
        }

        BackgroundJob job = BackgroundJob.queuedDeduplicated(
                user,
                jobType,
                priority,
                Map.of(
                        "provider", "github",
                        "repositoryId", repositoryId.toString()
                ),
                5,
                OffsetDateTime.now(ZoneOffset.UTC),
                deduplicationKey
        );
        jobs.persist(job);
        return job;
    }
}
