package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.*;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.persistence.repository.ContributionSyncRunRepository;
import io.github.developeranalytics.provider.*;
import io.github.developeranalytics.provider.github.GitHubContributorSnapshotService;
import io.github.developeranalytics.provider.github.GitHubProviderAdapter;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import io.github.developeranalytics.service.connection.ProviderSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import io.github.developeranalytics.observability.StructuredLog;
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
    @Inject ProviderCredentialService credentials;
    @Inject ContributionRepository contributions;
    @Inject ContributionSyncRunRepository syncRuns;
    @Inject GitHubCommitFileChangeService commitFileChanges;

    @Transactional
    public DiscoveryResult discover(AppUser user, SourceRepository repository, OffsetDateTime since)
            throws ProviderException {
        ProviderSession providerSession = credentials.requireSession(user.getId(), "github");
        ProviderAccessToken token = providerSession.accessToken();
        String userLogin = providerSession.login();
        if (userLogin == null || userLogin.isBlank()) {
            userLogin = github.fetchCurrentUser(token).login();
        }

        ProviderRepository providerRepository = new ProviderRepository(
                repository.getExternalRepositoryId(), repository.getOwnerExternalId(), repository.getOwnerLogin(),
                mapOwnerType(repository), repository.getName(), repository.getFullName(), repository.getHtmlUrl(),
                repository.getVisibility() == RepositoryVisibility.PRIVATE
                        ? ProviderRepository.Visibility.PRIVATE : ProviderRepository.Visibility.PUBLIC,
                repository.isFork(), repository.isArchived(), null, null, repository.getLastActivityAt());

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

    private ProviderRepository.OwnerType mapOwnerType(SourceRepository repository) {
        return repository.getOwnerLogin() == null ? ProviderRepository.OwnerType.OTHER : ProviderRepository.OwnerType.USER;
    }

    public record DiscoveryResult(UUID syncRunId, UUID repositoryId, int seen, int created, int updated, int pagesProcessed) {}
}
