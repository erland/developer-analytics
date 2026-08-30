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

@ApplicationScoped
public class ContributionSyncOrchestrator {

    public static final int DEFAULT_BATCH_SIZE = 25;
    public static final int MAX_BATCH_SIZE = 100;

    @Inject
    SourceRepositoryRepository repositories;

    @Inject
    RepositoryDiscoveryJobService jobs;

    @Transactional
    public BatchResult enqueueBatch(
            AppUser user,
            int offset,
            int requestedBatchSize
    ) {
        int batchSize = Math.max(
                1,
                Math.min(requestedBatchSize, MAX_BATCH_SIZE)
        );

        List<SourceRepository> candidates =
                repositories.findContributionSyncCandidates(
                        user.getId(),
                        Math.max(offset, 0),
                        batchSize
                );

        int queued = 0;
        int alreadyQueued = 0;

        for (SourceRepository repository : candidates) {
            BackgroundJob job =
                    jobs.enqueueContributionDiscovery(user, repository.getId());

            if (job == null) {
                alreadyQueued++;
            } else {
                queued++;
            }
        }

        return new BatchResult(
                candidates.size(),
                queued,
                alreadyQueued,
                candidates.size() == batchSize
                        ? Math.max(offset, 0) + candidates.size()
                        : null
        );
    }

    public record BatchResult(
            int repositoriesConsidered,
            int jobsQueued,
            int alreadyQueued,
            Integer nextOffset
    ) {
    }
}
