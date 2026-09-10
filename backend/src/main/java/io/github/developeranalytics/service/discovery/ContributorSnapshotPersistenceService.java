package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.provider.ProviderContributorSnapshot;
import io.github.developeranalytics.provider.ProviderContributorStatistics;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.UUID;

/** Persists contributor totals from one provider snapshot. */
@ApplicationScoped
public class ContributorSnapshotPersistenceService {

    @Inject SourceRepositoryRepository repositories;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void persist(UUID userId, SourceRepository repository, ProviderContributorSnapshot snapshot) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (repository == null) throw new IllegalArgumentException("repository is required");
        if (snapshot == null || snapshot.statistics() == null) {
            throw new IllegalArgumentException("snapshot statistics are required");
        }

        SourceRepository managedRepository = repositories.findByIdForUser(repository.getId(), userId)
                .orElseThrow(() -> new IllegalStateException("Repository not found for contributor snapshot persistence"));
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
    }
}
