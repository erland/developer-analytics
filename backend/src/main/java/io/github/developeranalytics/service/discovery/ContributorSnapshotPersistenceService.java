package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.provider.ProviderContributorSnapshot;
import io.github.developeranalytics.provider.ProviderContributorStatistics;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.UUID;

/** Persists contributor totals and weekly activity from one provider snapshot. */
@ApplicationScoped
public class ContributorSnapshotPersistenceService {

    @Inject GitHubWeeklyActivityService weeklyActivity;
    @Inject EntityManager entityManager;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void persist(UUID userId, SourceRepository repository, ProviderContributorSnapshot snapshot) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (repository == null) throw new IllegalArgumentException("repository is required");
        if (snapshot == null || snapshot.statistics() == null) {
            throw new IllegalArgumentException("snapshot statistics are required");
        }

        SourceRepository managedRepository = entityManager.merge(repository);
        ProviderContributorStatistics statistics = snapshot.statistics();
        managedRepository.updateContributorStatistics(
                statistics.contributorCount(),
                statistics.humanContributorCount(),
                statistics.botContributorCount(),
                statistics.userCommitCount(),
                statistics.repositoryCommitCount(),
                statistics.userAdditions(),
                statistics.userDeletions(),
                statistics.observedAt()
        );
        weeklyActivity.replace(userId, managedRepository, snapshot.userActivityWeeks());
    }
}
