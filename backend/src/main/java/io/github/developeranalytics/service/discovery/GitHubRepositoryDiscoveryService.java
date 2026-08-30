package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.*;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.persistence.repository.RepositorySyncRunRepository;
import io.github.developeranalytics.provider.*;
import io.github.developeranalytics.provider.github.GitHubProviderAdapter;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@ApplicationScoped
public class GitHubRepositoryDiscoveryService {

    @Inject
    GitHubProviderAdapter github;

    @Inject
    ProviderCredentialService credentials;

    @Inject
    SourceRepositoryRepository repositories;

    @Inject
    RepositorySyncRunRepository syncRuns;


@Transactional
public DiscoveryResult discover(AppUser user) throws ProviderException {
    ProviderAccessToken token = credentials.requireAccessToken(user.getId(), "github");
    ProviderUser providerUser = github.fetchCurrentUser(token);

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
    RepositorySyncRun run = new RepositorySyncRun(user, "github");
    syncRuns.persist(run);
    run.start(now);

    int seen = 0;
    int created = 0;
    int updated = 0;
    int pages = 0;
    String cursor = null;

    try {
        do {
            PagedResult<ProviderRepository> page = github.listRepositories(token, cursor);
            pages++;

            for (ProviderRepository providerRepository : page.items()) {
                SourceRepository repository = repositories
                        .findByExternalIdForUser(
                                user.getId(),
                                "github",
                                providerRepository.externalRepositoryId())
                        .orElse(null);

                if (repository == null) {
                    repository = new SourceRepository(
                            user,
                            "github",
                            providerRepository.externalRepositoryId(),
                            providerRepository.ownerLogin(),
                            providerRepository.name()
                    );
                    repositories.persist(repository);
                    created++;
                } else {
                    updated++;
                }

                repository.markSyncing();
                repository.updateFromDiscovery(
                        providerRepository.ownerExternalId(),
                        providerRepository.ownerLogin(),
                        providerRepository.name(),
                        providerRepository.fullName(),
                        providerRepository.htmlUrl(),
                        mapOwnerType(providerRepository.ownerType()),
                        ownership(providerUser, providerRepository),
                        mapVisibility(providerRepository.visibility()),
                        providerRepository.fork(),
                        providerRepository.archived(),
                        latestActivity(providerRepository),
                        now
                );
                repository.markSynced(now);
                seen++;
            }

            ProviderRateLimit rate = page.rateLimit();
            run.progress(
                    seen,
                    created,
                    updated,
                    pages,
                    rate == null ? null : rate.remaining(),
                    rate == null ? null : rate.resetAt()
            );

            cursor = page.nextCursor();
        } while (cursor != null);

        run.complete(OffsetDateTime.now(ZoneOffset.UTC));
        return new DiscoveryResult(run.getId(), seen, created, updated, pages);
    } catch (ProviderException e) {
        OffsetDateTime failedAt = OffsetDateTime.now(ZoneOffset.UTC);
        if (e.getStatusCode() == 403 || e.getStatusCode() == 429) {
            run.rateLimited(e.getMessage(), run.getRateLimitResetAt(), failedAt);
        } else {
            run.fail(e.getMessage(), failedAt);
        }
        throw e;
    } catch (RuntimeException e) {
        run.fail(e.getMessage(), OffsetDateTime.now(ZoneOffset.UTC));
        throw e;
    }
}

    private RepositoryOwnershipRelation ownership(
            ProviderUser currentUser,
            ProviderRepository repository
    ) {
        if (repository.ownerExternalId() != null &&
                repository.ownerExternalId().equals(currentUser.externalUserId())) {
            return RepositoryOwnershipRelation.OWNED_BY_USER;
        }
        if (repository.ownerType() == ProviderRepository.OwnerType.ORGANIZATION) {
            return RepositoryOwnershipRelation.ORGANIZATION_OWNED;
        }
        return RepositoryOwnershipRelation.EXTERNAL;
    }

    private RepositoryOwnerType mapOwnerType(ProviderRepository.OwnerType value) {
        return switch (value) {
            case USER -> RepositoryOwnerType.USER;
            case ORGANIZATION -> RepositoryOwnerType.ORGANIZATION;
            case OTHER -> RepositoryOwnerType.OTHER;
        };
    }

    private RepositoryVisibility mapVisibility(ProviderRepository.Visibility value) {
        return value == ProviderRepository.Visibility.PRIVATE
                ? RepositoryVisibility.PRIVATE
                : RepositoryVisibility.PUBLIC;
    }

    private OffsetDateTime latestActivity(ProviderRepository repository) {
        if (repository.pushedAt() != null) return repository.pushedAt();
        return repository.updatedAt();
    }

    public record DiscoveryResult(UUID syncRunId, int seen, int created, int updated, int pagesProcessed) {}
}
