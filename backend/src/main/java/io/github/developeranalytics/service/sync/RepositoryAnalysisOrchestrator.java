package io.github.developeranalytics.service.sync;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Queues the deterministic analysis pipeline for repositories that need it.
 * Normal discovery refreshes are incremental: a repository is re-analysed only
 * when it has never completed the current analysis version or GitHub reports
 * activity newer than the repository's analysis watermark. Explicit per-repo
 * refreshes still force the complete pipeline.
 */
@ApplicationScoped
public class RepositoryAnalysisOrchestrator {

    @Inject SourceRepositoryRepository repositories;
    @Inject RepositoryDiscoveryJobService jobs;

    @Transactional
    public QueueResult enqueueAll(AppUser user) {
        List<SourceRepository> selected = repositories.findAnalysisCandidates(user.getId());
        List<SourceRepository> candidates = selected.stream()
                .filter(SourceRepository::needsAnalysisRefresh)
                .toList();

        int repositoryJobsQueued = 0;
        int alreadyQueued = 0;
        for (SourceRepository repository : candidates) {
            QueueCounts counts = enqueueRepositoryJobs(user, repository);
            repositoryJobsQueued += counts.queued();
            alreadyQueued += counts.alreadyQueued();
        }

        int aggregateJobsQueued = repositoryJobsQueued > 0
                ? enqueueAggregateJobs(user)
                : 0;
        return new QueueResult(
                candidates.size(),
                repositoryJobsQueued,
                alreadyQueued,
                aggregateJobsQueued
        );
    }

    @Transactional
    public QueueResult enqueueRepository(AppUser user, UUID repositoryId) {
        SourceRepository repository = repositories.findByIdForUser(
                repositoryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Repository not found for user"));

        if (!repository.isIncludedInAnalysis()) {
            throw new IllegalStateException(
                    "Repository is not included in analysis");
        }

        QueueCounts counts = enqueueRepositoryJobs(user, repository);
        int aggregateJobsQueued = counts.queued() > 0
                ? enqueueAggregateJobs(user)
                : 0;
        return new QueueResult(1, counts.queued(), counts.alreadyQueued(), aggregateJobsQueued);
    }

    private QueueCounts enqueueRepositoryJobs(AppUser user, SourceRepository repository) {
        UUID repositoryId = repository.getId();
        int queued = 0;
        int alreadyQueued = 0;

        BackgroundJob[] repositoryJobs = new BackgroundJob[] {
                jobs.enqueueContributionDiscovery(user, repositoryId),
                jobs.enqueueLanguageEvidence(user, repositoryId),
                jobs.enqueueFileManifestEvidence(user, repositoryId),
                jobs.enqueueDeterministicClassification(
                        user,
                        repositoryId,
                        repository.getLastActivityAt()
                )
        };

        for (BackgroundJob job : repositoryJobs) {
            if (job == null) alreadyQueued++;
            else queued++;
        }

        return new QueueCounts(queued, alreadyQueued);
    }

    public int enqueueAggregateJobs(AppUser user) {
        int queued = 0;
        if (jobs.enqueueTechnologyAssessmentRecalculation(user) != null) queued++;
        if (jobs.enqueueTechnologyTimelineRecalculation(user) != null) queued++;
        if (jobs.enqueueProjectSignificanceRecalculation(user) != null) queued++;
        return queued;
    }

    private record QueueCounts(int queued, int alreadyQueued) {}

    public record QueueResult(
            int repositoriesConsidered,
            int repositoryJobsQueued,
            int alreadyQueued,
            int aggregateJobsQueued
    ) {}
}
