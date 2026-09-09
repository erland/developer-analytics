package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.ContributionSyncRun;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.observability.StructuredLog;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.persistence.repository.ContributionSyncRunRepository;
import io.github.developeranalytics.provider.PagedResult;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderContribution;
import io.github.developeranalytics.provider.ProviderContributorSnapshot;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRateLimit;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.github.GitHubContributorSnapshotService;
import io.github.developeranalytics.provider.github.GitHubProviderAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.util.UUID;

@ApplicationScoped
public class GitHubContributionDiscoveryService {
    private static final Logger LOG = Logger.getLogger(GitHubContributionDiscoveryService.class);

    @Inject GitHubProviderAdapter github;
    @Inject GitHubContributorSnapshotService contributorSnapshots;
    @Inject ContributorSnapshotPersistenceService contributorSnapshotPersistence;
    @Inject GitHubContributionIngestionService ingestion;
    @Inject GitHubContributionSyncContextResolver contextResolver;
    @Inject ContributionRepository contributions;
    @Inject ContributionSyncRunRepository syncRuns;
    @Inject GitHubCommitFileChangeService commitFileChanges;

    @Transactional
    public DiscoveryResult discover(AppUser user, SourceRepository repository, OffsetDateTime since)
            throws ProviderException {
        GitHubContributionSyncContextResolver.SyncContext context =
                contextResolver.resolve(user.getId(), repository);
        ProviderAccessToken token = context.accessToken();
        String userLogin = context.userLogin();
        ProviderRepository providerRepository = context.providerRepository();

        OffsetDateTime startedAt = OffsetDateTime.now(java.time.ZoneOffset.UTC);
        if (repository.getContributionScopeVersion() < 2 && since == null) {
            contributions.deleteForRepository(user.getId(), repository.getId());
        }
        repository.markSyncing();
        ContributionSyncRun run = new ContributionSyncRun(user, repository, "github");
        syncRuns.persist(run);
        run.start(startedAt);
        StructuredLog.info(LOG, "contribution_sync_started",
                StructuredLog.fields("syncId", run.getId(), "provider", "github", "repositoryId", repository.getId()));

        int seen = 0;
        int created = 0;
        int updated = 0;
        int pages = 0;
        String cursor = null;

        try {
            do {
                PagedResult<ProviderContribution> page =
                        github.listContributions(token, providerRepository, since, cursor, userLogin);
                pages++;

                for (ProviderContribution providerContribution : page.items()) {
                    GitHubContributionIngestionService.IngestionResult result =
                            ingestion.ingest(user, repository, providerContribution, token);
                    if (result.created()) created++;
                    if (result.updated()) updated++;
                    seen++;
                }

                ProviderRateLimit rate = page.rateLimit();
                run.progress(seen, created, updated, pages,
                        rate == null ? null : rate.remaining(), rate == null ? null : rate.resetAt());
                cursor = page.nextCursor();
            } while (cursor != null);

            try {
                ProviderContributorSnapshot snapshot = contributorSnapshots.fetch(
                        token, providerRepository, userLogin);
                contributorSnapshotPersistence.persist(user.getId(), repository, snapshot);
            } catch (ProviderException statisticsError) {
                if (statisticsError.getStatusCode() == 403 || statisticsError.getStatusCode() == 429) {
                    throw statisticsError;
                }
                StructuredLog.warn(LOG, "contributor_statistics_unavailable", statisticsError,
                        StructuredLog.fields("repositoryId", repository.getId(), "httpStatus", statisticsError.getStatusCode()));
            }

            OffsetDateTime completedAt = OffsetDateTime.now(java.time.ZoneOffset.UTC);
            run.complete(completedAt);
            if (!commitFileChanges.hasMissingCurrentClassification(user, repository)) {
                repository.markContributionScopeCurrent();
            }
            repository.markSynced(completedAt);
            StructuredLog.info(LOG, "contribution_sync_completed",
                    StructuredLog.fields("syncId", run.getId(), "repositoryId", repository.getId(),
                            "provider", "github", "seen", seen, "created", created, "updated", updated, "pages", pages));
            return new DiscoveryResult(run.getId(), repository.getId(), seen, created, updated, pages);
        } catch (ProviderException e) {
            StructuredLog.warn(LOG, "contribution_sync_provider_error", e,
                    StructuredLog.fields("syncId", run.getId(), "provider", "github",
                            "repositoryId", repository.getId(), "httpStatus", e.getStatusCode(),
                            "retryAt", e.getRetryAt()));
            OffsetDateTime failedAt = OffsetDateTime.now(java.time.ZoneOffset.UTC);
            repository.markSyncFailed(e.getMessage());
            if (e.getStatusCode() == 403 || e.getStatusCode() == 429) {
                OffsetDateTime resetAt = e.getRetryAt() != null ? e.getRetryAt() : run.getRateLimitResetAt();
                run.rateLimited(e.getMessage(), resetAt, failedAt);
            } else {
                run.fail(e.getMessage(), failedAt);
            }
            throw e;
        } catch (RuntimeException e) {
            StructuredLog.warn(LOG, "contribution_sync_runtime_error", e,
                    StructuredLog.fields("syncId", run.getId(), "provider", "github", "repositoryId", repository.getId()));
            repository.markSyncFailed(e.getMessage());
            run.fail(e.getMessage(), OffsetDateTime.now(java.time.ZoneOffset.UTC));
            throw e;
        }
    }

    public record DiscoveryResult(UUID syncRunId, UUID repositoryId, int seen, int created, int updated, int pagesProcessed) {}
}
