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

@ApplicationScoped
public class RepositoryAnalysisOrchestrator {

    @Inject SourceRepositoryRepository repositories;
    @Inject RepositoryDiscoveryJobService jobs;

    @Transactional
    public QueueResult enqueueAll(AppUser user) { return enqueueAll(user, null); }

    @Transactional
    public QueueResult enqueueAll(AppUser user, UUID providerSyncRunId) {
        List<SourceRepository> selected = repositories.findAnalysisCandidates(user.getId());
        List<SourceRepository> candidates = selected.stream().filter(SourceRepository::needsAnalysisRefresh).toList();
        int repositoryJobsQueued = 0;
        int contributionJobsQueued = 0;
        int alreadyQueued = 0;
        for (SourceRepository repository : candidates) {
            QueueCounts counts = enqueueRepositoryJobs(user, repository, providerSyncRunId);
            repositoryJobsQueued += counts.queued();
            contributionJobsQueued += counts.contributionQueued();
            alreadyQueued += counts.alreadyQueued();
        }
        int aggregateJobsQueued = repositoryJobsQueued > 0 ? enqueueAggregateJobs(user) : 0;
        return new QueueResult(candidates.size(), repositoryJobsQueued, contributionJobsQueued, alreadyQueued, aggregateJobsQueued);
    }

    @Transactional
    public QueueResult enqueueRepository(AppUser user, UUID repositoryId) {
        SourceRepository repository = repositories.findByIdForUser(repositoryId, user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Repository not found for user"));
        if (!repository.isIncludedInAnalysis()) throw new IllegalStateException("Repository is not included in analysis");
        QueueCounts counts = enqueueRepositoryJobs(user, repository, null);
        int aggregateJobsQueued = counts.queued() > 0 ? enqueueAggregateJobs(user) : 0;
        return new QueueResult(1, counts.queued(), counts.contributionQueued(), counts.alreadyQueued(), aggregateJobsQueued);
    }

    private QueueCounts enqueueRepositoryJobs(AppUser user, SourceRepository repository, UUID providerSyncRunId) {
        UUID repositoryId = repository.getId();
        int queued = 0, alreadyQueued = 0, contributionQueued = 0;
        BackgroundJob contribution = jobs.enqueueContributionDiscovery(user, repositoryId, providerSyncRunId);
        if (contribution == null) alreadyQueued++; else { queued++; contributionQueued++; }
        BackgroundJob[] otherJobs = new BackgroundJob[] {
                jobs.enqueueLanguageEvidence(user, repositoryId),
                jobs.enqueueFileManifestEvidence(user, repositoryId),
                jobs.enqueueDeterministicClassification(user, repositoryId, repository.getLastActivityAt())
        };
        for (BackgroundJob job : otherJobs) { if (job == null) alreadyQueued++; else queued++; }
        return new QueueCounts(queued, contributionQueued, alreadyQueued);
    }

    public int enqueueAggregateJobs(AppUser user) {
        int queued = 0;
        if (jobs.enqueueTechnologyAssessmentRecalculation(user) != null) queued++;
        if (jobs.enqueueTechnologyTimelineRecalculation(user) != null) queued++;
        if (jobs.enqueueProjectSignificanceRecalculation(user) != null) queued++;
        return queued;
    }

    private record QueueCounts(int queued, int contributionQueued, int alreadyQueued) {}

    public record QueueResult(int repositoriesConsidered, int repositoryJobsQueued, int contributionJobsQueued,
                              int alreadyQueued, int aggregateJobsQueued) {}
}
