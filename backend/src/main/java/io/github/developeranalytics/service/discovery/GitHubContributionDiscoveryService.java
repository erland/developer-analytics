package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.ContributionSyncMode;
import io.github.developeranalytics.domain.model.ProviderSyncRun;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.observability.StructuredLog;
import io.github.developeranalytics.provider.PagedResult;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderContribution;
import io.github.developeranalytics.provider.ProviderContributorSnapshot;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRateLimit;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.github.GitHubApiUsageTracker;
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
    public static final String CONTRIBUTOR_STATS_CONTINUATION = "@contributor-stats";

    @Inject GitHubProviderAdapter github;
    @Inject GitHubContributorSnapshotService contributorSnapshots;
    @Inject ContributorSnapshotPersistenceService contributorSnapshotPersistence;
    @Inject GitHubContributionIngestionService ingestion;
    @Inject GitHubContributionSyncContextResolver contextResolver;
    @Inject GitHubContributionDiscoveryPersistenceService persistence;
    @Inject GitHubRateLimitService rateLimits;
    @Inject GitHubApiUsageTracker apiUsage;

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public DiscoveryResult discover(AppUser user, SourceRepository repository, OffsetDateTime since) throws ProviderException {
        ContributionSyncMode mode = since == null ? ContributionSyncMode.INITIAL_FULL : ContributionSyncMode.INCREMENTAL;
        return discover(user, repository, since, null, ignored -> {}, mode, null);
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public DiscoveryResult discover(AppUser user, SourceRepository repository, OffsetDateTime since,
                                    String initialCursor, Consumer<String> checkpoint,
                                    ContributionSyncMode syncMode) throws ProviderException {
        return discover(user, repository, since, initialCursor, checkpoint, syncMode, null);
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public DiscoveryResult discover(AppUser user, SourceRepository repository, OffsetDateTime since,
                                    String initialCursor, Consumer<String> checkpoint,
                                    ContributionSyncMode syncMode, ProviderSyncRun providerSyncRun) throws ProviderException {
        GitHubContributionSyncContextResolver.SyncContext context = contextResolver.resolve(user.getId(), repository);
        ProviderAccessToken token = context.accessToken();
        String userLogin = context.userLogin();
        ProviderRepository providerRepository = context.providerRepository();
        ContributionSyncMode effectiveMode = syncMode == null ? ContributionSyncMode.UNKNOWN : syncMode;
        boolean contributorStatsOnly = isContributorStatsContinuation(initialCursor);

        OffsetDateTime startedAt = OffsetDateTime.now(ZoneOffset.UTC);
        boolean resetExisting = !contributorStatsOnly
                && repository.getContributionScopeVersion() < 2
                && since == null
                && initialCursor == null;
        UUID runId = persistence.start(user, repository, effectiveMode, providerSyncRun, resetExisting, startedAt);
        StructuredLog.info(LOG, "contribution_sync_started",
                StructuredLog.fields("syncId", runId, "providerSyncRunId", providerSyncRun == null ? null : providerSyncRun.getId(),
                        "provider", "github", "repositoryId", repository.getId(), "syncMode", effectiveMode,
                        "resumeCursor", initialCursor, "contributorStatsOnly", contributorStatsOnly));

        int seen = 0, created = 0, updated = 0, pages = 0;
        String cursor = contributorStatsOnly ? null : blankToNull(initialCursor);

        try {
            if (!contributorStatsOnly) {
                do {
                    PagedResult<ProviderContribution> page = github.listContributions(token, providerRepository, since, cursor, userLogin);
                    pages++;
                    for (ProviderContribution providerContribution : page.items()) {
                        GitHubContributionIngestionService.IngestionResult result = ingestion.ingest(user, repository, providerContribution, token);
                        if (result.created()) created++;
                        if (result.updated()) updated++;
                        seen++;
                    }
                    ProviderRateLimit rate = page.rateLimit();
                    persistence.progress(user.getId(), runId, seen, created, updated, pages,
                            rate == null ? null : rate.remaining(), rate == null ? null : rate.resetAt());
                    cursor = blankToNull(page.nextCursor());
                    checkpoint.accept(cursor);
                    if (cursor != null) {
                        GitHubRateLimitService.GitHubRateLimitDecision decision = rateLimits.decision(token);
                        if (!decision.allowed()) {
                            captureApiUsage(user.getId(), runId);
                            persistence.pause(user.getId(), runId, decision.resumeAt(), OffsetDateTime.now(ZoneOffset.UTC));
                            return new DiscoveryResult(runId, repository.getId(), seen, created, updated, pages, false, cursor, decision.resumeAt());
                        }
                    }
                } while (cursor != null);
                checkpoint.accept(CONTRIBUTOR_STATS_CONTINUATION);
            }

            try {
                ProviderContributorSnapshot snapshot = contributorSnapshots.fetch(token, providerRepository, userLogin);
                contributorSnapshotPersistence.persist(user.getId(), repository, snapshot);
            } catch (ProviderException statisticsError) {
                if (isRateLimit(statisticsError, token)) {
                    OffsetDateTime resetAt = retryAt(statisticsError, token);
                    captureApiUsage(user.getId(), runId);
                    persistence.pause(user.getId(), runId, resetAt, OffsetDateTime.now(ZoneOffset.UTC));
                    return new DiscoveryResult(runId, repository.getId(), seen, created, updated, pages, false,
                            CONTRIBUTOR_STATS_CONTINUATION, resetAt);
                }
                StructuredLog.warn(LOG, "contributor_statistics_unavailable", statisticsError,
                        StructuredLog.fields("repositoryId", repository.getId(), "syncMode", effectiveMode,
                                "httpStatus", statisticsError.getStatusCode()));
            }

            checkpoint.accept(null);
            OffsetDateTime completedAt = OffsetDateTime.now(ZoneOffset.UTC);
            GitHubApiUsageTracker.UsageSnapshot usage = captureApiUsage(user.getId(), runId);
            persistence.complete(user.getId(), repository.getId(), runId, completedAt);
            StructuredLog.info(LOG, "contribution_sync_completed",
                    StructuredLog.fields("syncId", runId, "providerSyncRunId", providerSyncRun == null ? null : providerSyncRun.getId(),
                            "repositoryId", repository.getId(), "provider", "github", "syncMode", effectiveMode,
                            "seen", seen, "created", created, "updated", updated, "pages", pages,
                            "apiRequests", usage.totalRequests(), "apiRequestsByEndpoint", usage.requestsByEndpoint()));
            return new DiscoveryResult(runId, repository.getId(), seen, created, updated, pages, true, null, null);
        } catch (ProviderException e) {
            captureApiUsage(user.getId(), runId);
            if (isRateLimit(e, token)) {
                OffsetDateTime resetAt = retryAt(e, token);
                String nextCursor = contributorStatsOnly ? CONTRIBUTOR_STATS_CONTINUATION : cursor;
                persistence.pause(user.getId(), runId, resetAt, OffsetDateTime.now(ZoneOffset.UTC));
                return new DiscoveryResult(runId, repository.getId(), seen, created, updated, pages, false, nextCursor, resetAt);
            }
            persistence.fail(user.getId(), repository.getId(), runId, e.getMessage(), OffsetDateTime.now(ZoneOffset.UTC));
            throw e;
        } catch (RuntimeException e) {
            captureApiUsage(user.getId(), runId);
            persistence.fail(user.getId(), repository.getId(), runId, e.getMessage(), OffsetDateTime.now(ZoneOffset.UTC));
            throw e;
        }
    }

    static boolean isContributorStatsContinuation(String cursor) { return CONTRIBUTOR_STATS_CONTINUATION.equals(cursor); }

    private GitHubApiUsageTracker.UsageSnapshot captureApiUsage(UUID userId, UUID runId) {
        GitHubApiUsageTracker.UsageSnapshot snapshot = apiUsage.snapshot();
        persistence.apiUsage(userId, runId, snapshot.totalRequests(), snapshot.requestsByEndpoint());
        return snapshot;
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

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }

    public record DiscoveryResult(UUID syncRunId, UUID repositoryId, int seen, int created, int updated,
                                  int pagesProcessed, boolean complete, String nextCursor, OffsetDateTime resumeAt) {}
}
