package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.*;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.persistence.repository.ContributionSyncRunRepository;
import io.github.developeranalytics.provider.*;
import io.github.developeranalytics.provider.github.GitHubProviderAdapter;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
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
    @Inject ProviderCredentialService credentials;
    @Inject ContributionRepository contributions;
    @Inject ContributionSyncRunRepository syncRuns;
    @Inject GitHubWeeklyActivityService weeklyActivity;

    @Transactional
    public DiscoveryResult discover(AppUser user, SourceRepository repository, OffsetDateTime since)
            throws ProviderException {
        ProviderAccessToken token = credentials.requireAccessToken(user.getId(), "github");
        ProviderUser providerUser = github.fetchCurrentUser(token);

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
                        github.listContributions(token, providerRepository, since, cursor, providerUser.login());
                pages++;

                for (ProviderContribution pc : page.items()) {
                    Contribution.Type type = mapType(pc.type());
                    Contribution contribution = contributions.findByProviderIdentity(
                            user.getId(), "github", pc.externalContributionId(), type).orElse(null);
                    if (contribution == null) {
                        contribution = new Contribution(user, repository, "github",
                                pc.externalContributionId(), type, pc.occurredAt());
                        contributions.persist(contribution);
                        created++;
                    } else {
                        updated++;
                    }
                    contribution.updateFromDiscovery(pc.title(), pc.occurredAt(), mapState(pc.state()),
                            pc.additions(), pc.deletions(), pc.changedFiles(), pc.merged());
                    seen++;
                }

                ProviderRateLimit rate = page.rateLimit();
                run.progress(seen, created, updated, pages,
                        rate == null ? null : rate.remaining(), rate == null ? null : rate.resetAt());
                cursor = page.nextCursor();
            } while (cursor != null);

            try {
                ProviderContributorStatistics statistics = github.fetchContributorStatistics(
                        token, providerRepository, providerUser.login());
                repository.updateContributorStatistics(
                        statistics.contributorCount(), statistics.humanContributorCount(), statistics.botContributorCount(),
                        statistics.userCommitCount(), statistics.repositoryCommitCount(), statistics.userAdditions(),
                        statistics.userDeletions(), statistics.observedAt());
            } catch (ProviderException statisticsError) {
                StructuredLog.warn(LOG, "contributor_statistics_unavailable", statisticsError,
                        StructuredLog.fields("repositoryId", repository.getId(), "httpStatus", statisticsError.getStatusCode()));
            }

            // GitHub's commit list does not contain additions/deletions. The contributor
            // statistics endpoint does, grouped by week, so keep that separately for time charts.
            weeklyActivity.refresh(user.getId(), repository, token, providerUser.login());

            OffsetDateTime completedAt = OffsetDateTime.now(java.time.ZoneOffset.UTC);
            run.complete(completedAt);
            repository.markContributionScopeCurrent();
            repository.markSynced(completedAt);
            StructuredLog.info(LOG, "contribution_sync_completed",
                    StructuredLog.fields("syncId", run.getId(), "repositoryId", repository.getId(),
                            "provider", "github", "seen", seen, "created", created, "updated", updated, "pages", pages));
            return new DiscoveryResult(run.getId(), repository.getId(), seen, created, updated, pages);
        } catch (ProviderException e) {
            StructuredLog.warn(LOG, "contribution_sync_provider_error", e,
                    StructuredLog.fields("syncId", run.getId(), "provider", "github",
                            "repositoryId", repository.getId(), "httpStatus", e.getStatusCode()));
            OffsetDateTime failedAt = OffsetDateTime.now(java.time.ZoneOffset.UTC);
            repository.markSyncFailed(e.getMessage());
            if (e.getStatusCode() == 403 || e.getStatusCode() == 429) {
                run.rateLimited(e.getMessage(), run.getRateLimitResetAt(), failedAt);
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

    private Contribution.Type mapType(ProviderContribution.Type type) {
        return switch (type) {
            case COMMIT -> Contribution.Type.COMMIT;
            case PULL_REQUEST -> Contribution.Type.PULL_REQUEST;
            case REVIEW -> Contribution.Type.REVIEW;
            case ISSUE -> Contribution.Type.ISSUE;
        };
    }

    private Contribution.State mapState(ProviderContribution.State state) {
        return switch (state) {
            case OPEN -> Contribution.State.OPEN;
            case CLOSED -> Contribution.State.CLOSED;
            case MERGED -> Contribution.State.MERGED;
            case UNKNOWN -> Contribution.State.UNKNOWN;
        };
    }

    private ProviderRepository.OwnerType mapOwnerType(SourceRepository repository) {
        return repository.getOwnerLogin() == null ? ProviderRepository.OwnerType.OTHER : ProviderRepository.OwnerType.USER;
    }

    public record DiscoveryResult(UUID syncRunId, UUID repositoryId, int seen, int created, int updated, int pagesProcessed) {}
}
