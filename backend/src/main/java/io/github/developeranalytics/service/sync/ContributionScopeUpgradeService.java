package io.github.developeranalytics.service.sync;

import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class ContributionScopeUpgradeService {

    static final int DEFAULT_SCAN_LIMIT = 100;

    @Inject SourceRepositoryRepository repositories;
    @Inject RepositoryDiscoveryJobService jobs;

    @Transactional
    public int enqueueMissingBackfills() {
        return enqueueMissingBackfills(DEFAULT_SCAN_LIMIT);
    }

    int enqueueMissingBackfills(int limit) {
        int enqueued = 0;
        for (SourceRepository repository : repositories.findContributionScopeUpgradeCandidates(
                limit, ChangeKindClassifier.CLASSIFIER_VERSION)) {
            if (repository.getId() == null || repository.getUser() == null) {
                continue;
            }
            if (jobs.enqueueChangeKindBackfill(repository.getUser(), repository.getId()) != null) {
                enqueued++;
            }
        }
        return enqueued;
    }
}
