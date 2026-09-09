package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.ContributionSyncMode;
import io.github.developeranalytics.domain.model.ContributionSyncRun;
import io.github.developeranalytics.domain.model.ProviderSyncRun;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.persistence.repository.ContributionSyncRunRepository;
import io.github.developeranalytics.persistence.repository.ProviderSyncRunRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps contribution-discovery database mutations in short, independent transactions.
 * Provider HTTP calls must stay outside these methods so a slow GitHub request cannot
 * hold a database transaction open until Narayana cancels it.
 */
@ApplicationScoped
public class GitHubContributionDiscoveryPersistenceService {

    @Inject ContributionRepository contributions;
    @Inject ContributionSyncRunRepository syncRuns;
    @Inject SourceRepositoryRepository repositories;
    @Inject ProviderSyncRunRepository providerSyncRuns;
    @Inject GitHubCommitFileChangeService commitFileChanges;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public UUID start(AppUser user, SourceRepository repository, ContributionSyncMode syncMode,
                      ProviderSyncRun providerSyncRun, boolean resetExisting, OffsetDateTime startedAt) {
        SourceRepository managedRepository = repositories.findByIdForUser(repository.getId(), user.getId())
                .orElseThrow(() -> new IllegalStateException("Repository not found for contribution sync"));
        if (resetExisting) contributions.deleteForRepository(user.getId(), repository.getId());
        managedRepository.markSyncing();

        ProviderSyncRun managedProviderSyncRun = providerSyncRun == null ? null
                : providerSyncRuns.findById(providerSyncRun.getId()).orElse(null);
        ContributionSyncRun run = new ContributionSyncRun(
                managedRepository.getUser(), managedRepository, "github", syncMode, managedProviderSyncRun);
        syncRuns.persist(run);
        run.start(startedAt);
        return run.getId();
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void progress(UUID userId, UUID runId, int seen, int created, int updated, int pages,
                         Integer remaining, OffsetDateTime resetAt) {
        run(userId, runId).progress(seen, created, updated, pages, remaining, resetAt);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void apiUsage(UUID userId, UUID runId, int requestCount, Map<String, Integer> requestsByEndpoint) {
        run(userId, runId).apiUsage(requestCount, requestsByEndpoint);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void pause(UUID userId, UUID runId, OffsetDateTime resetAt, OffsetDateTime now) {
        run(userId, runId).pauseForRateLimit(resetAt, now);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void complete(UUID userId, UUID repositoryId, UUID runId, OffsetDateTime completedAt) {
        run(userId, runId).complete(completedAt);
        SourceRepository repository = repository(userId, repositoryId);
        if (!commitFileChanges.hasMissingCurrentClassification(repository.getUser(), repository)) {
            repository.markContributionScopeCurrent();
        }
        repository.markSynced(completedAt);
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void fail(UUID userId, UUID repositoryId, UUID runId, String error, OffsetDateTime now) {
        repository(userId, repositoryId).markSyncFailed(error);
        run(userId, runId).fail(error, now);
    }

    private ContributionSyncRun run(UUID userId, UUID runId) {
        return syncRuns.findByIdForUser(runId, userId)
                .orElseThrow(() -> new IllegalStateException("Contribution sync run not found"));
    }

    private SourceRepository repository(UUID userId, UUID repositoryId) {
        return repositories.findByIdForUser(repositoryId, userId)
                .orElseThrow(() -> new IllegalStateException("Repository not found for contribution sync"));
    }
}
