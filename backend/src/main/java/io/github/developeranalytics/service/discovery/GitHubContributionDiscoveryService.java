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
import io.github.developeranalytics.provider.github.GitHubRateLimitService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.function.Consumer;

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
    @Inject GitHubRateLimitService rateLimits;

    @Transactional
    public DiscoveryResult discover(AppUser user, SourceRepository repository, OffsetDateTime since)
            throws ProviderException {
        return discover(user, repository, since, null, ignored -> {});
    }

    @Transactional
    public DiscoveryResult discover(
            AppUser user,
            SourceRepository repository,
            OffsetDateTime since,
            String initialCursor,
            Consumer<String> checkpoint
    ) throws ProviderException {
        GitHubContributionSyncContextResolver.SyncContext context =
                contextResolver.resolve(user.getId(), repository);
        ProviderAccessToken token = context.accessToken();
        String userLogin = context.userLogin();
        ProviderRepository providerRepository = context.providerRepository();

        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (repository.getContributionScopeVersion() < 2 && since == null && initialCursor == null) {
            contributions.deleteForRepository(user.getId(), repository.getId());
        }
        repository.markSyncing();
        ContributionSyncRun run = new ContributionSyncRun(user, repository, "github");
        syncRuns.persist(run);
        run.start(startedAt);
        StructuredLog.info(LOG, "contribution_sync_started",
                StructuredLog.fields("syncId", run.getId(), "provider", "github", "repositoryId", repository.getId(),
                        "resumeCursor", initialCursor));

        int seen = 0;
        int created = 0;
        int updated = 0;
        int pages = 0;
        String cursor = blankToNull(initialCursor);

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
                cursor = blankToNull(page.nextCursor());
                checkpoint.accept(cursor);

                if (cursor != null) {
                    GitHubRateLimitService.GitHubRateLimitDecision decision = rateLimits.decision(token);
                    if (!decision.allowed()) {
                        OffsetDateTime pausedAt = OffsetDateTime.now(ZoneOffset.UTC);
                        run.pauseForRateLimit(decision.resumeAt(), pausedAt);
                        StructuredLog.info(LOG, "contribution_sync_paused_rate_limit",
                                StructuredLog.fields("syncId", run.getId(), "repositoryId", repository.getId(),
                                        "nextCursor", cursor, "resumeAt", decision.resumeAt(),
                                        "remaining", decision.remaining(), "reserve", decision.reserve()));
                        return new DiscoveryResult(run.getId(), repository.getId(), seen, created, updated, pages,
                                false, cursor, decision.resumeAt());
                    }
                }
            } while (cursor != null);

            try {
                ProviderContributorSnapshot snapshot = contributorSnapshots.fetch(
                        token, providerRepository, userLogin);
                contributorSnapshotPersistence.persist(user.getId(), repository, snapshot);
            } catch (ProviderException statisticsError) {
                if (isRateLimit(statisticsError, token)) {
                    OffsetDateTime resetAt = retryAt(statisticsError, token);
                    run.pauseForRateLimit(resetAt, OffsetDateTime.now(ZoneOffset.UTC));
                    return new DiscoveryResult(run.getId(), repository.getId(), seen, created, updated, pages,
                            false, null, resetAt);
                }
                StructuredLog.warn(LOG, "contributor_statistics_unavailable", statisticsError,
                        StructuredLog.fields("repositoryId", repository.getId(), "httpStatus", statisticsError.getStatusCode()));
            }

            OffsetDateTime completedAt = OffsetDateTime.now(ZoneOffset.UTC);
            run.complete(completedAt);
            if (!commitFileChanges.hasMissingCurrentClassification(user, repository)) {
                repository.markContributionScopeCurrent();
            }
            repository.markSynced(completedAt);
            StructuredLog.info(LOG, "contribution_sync_completed",
                    StructuredLog.fields("syncId", run.getId(), "repositoryId", repository.getId(),
                            "provider", "github", "seen", seen, "created", created, "updated", updated, "pages", pages));
            return new DiscoveryResult(run.getId(), repository.getId(), seen, created, updated, pages,
                    true, null, null);
        } catch (ProviderException e) {
            if (isRateLimit(e, token)) {
                OffsetDateTime resetAt = retryAt(e, token);
                run.pauseForRateLimit(resetAt, OffsetDateTime.now(ZoneOffset.UTC));
                StructuredLog.info(LOG, "contribution_sync_paused_rate_limit",
                        StructuredLog.fields("syncId", run.getId(), "provider", "github",
                                "repositoryId", repository.getId(), "httpStatus", e.getStatusCode(),
                                "nextCursor", cursor, "resumeAt", resetAt));
                return new DiscoveryResult(run.getId(), repository.getId(), seen, created, updated, pages,
                        false, cursor, resetAt);
            }

            StructuredLog.warn(LOG, "contribution_sync_provider_error", e,
                    StructuredLog.fields("syncId", run.getId(), "provider", "github",
                            "repositoryId", repository.getId(), "httpStatus", e.getStatusCode(),
                            "retryAt", e.getRetryAt()));
            OffsetDateTime failedAt = OffsetDateTime.now(ZoneOffset.UTC);
            repository.markSyncFailed(e.getMessage());
            run.fail(e.getMessage(), failedAt);
            throw e;
        } catch (RuntimeException e) {
            StructuredLog.warn(LOG, "contribution_sync_runtime_error", e,
                    StructuredLog.fields("syncId", run.getId(), "provider", "github", "repositoryId", repository.getId()));
            repository.markSyncFailed(e.getMessage());
            run.fail(e.getMessage(), OffsetDateTime.now(ZoneOffset.UTC));
            throw e;
        }
    }

    private boolean isRateLimit(ProviderException error, ProviderAccessToken token) {
        if (error.getStatusCode() == 429 || error.getRetryAt() != null) return true;
        if (error.getStatusCode() != 403) return false;
        return !rateLimits.decision(token).allowed();
    }

    private OffsetDateTime retryAt(ProviderException error, ProviderAccessToken token) {
        if (error.getRetryAt() != null) return error.getRetryAt();
        GitHubRateLimitService.GitHubRateLimitDecision decision = rateLimits.decision(token);
        if (decision.resumeAt() != null) return decision.resumeAt();
        return OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    public record DiscoveryResult(
            UUID syncRunId,
            UUID repositoryId,
            int seen,
            int created,
            int updated,
            int pagesProcessed,
            boolean complete,
            String nextCursor,
            OffsetDateTime resumeAt
    ) {}
}
