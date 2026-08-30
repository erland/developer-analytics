package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.*;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.provider.*;
import io.github.developeranalytics.provider.github.GitHubProviderAdapter;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@ApplicationScoped
public class GitHubContributionDiscoveryService {

    @Inject
    GitHubProviderAdapter github;

    @Inject
    ProviderCredentialService credentials;

    @Inject
    ContributionRepository contributions;

    @Transactional
    public DiscoveryResult discover(AppUser user, SourceRepository repository, OffsetDateTime since)
            throws ProviderException {
        ProviderAccessToken token = credentials.requireAccessToken(user.getId(), "github");

        ProviderRepository providerRepository = new ProviderRepository(
                repository.getExternalRepositoryId(),
                repository.getOwnerExternalId(),
                repository.getOwnerLogin(),
                mapOwnerType(repository),
                repository.getName(),
                repository.getFullName(),
                repository.getHtmlUrl(),
                repository.getVisibility() == RepositoryVisibility.PRIVATE
                        ? ProviderRepository.Visibility.PRIVATE
                        : ProviderRepository.Visibility.PUBLIC,
                repository.isFork(),
                repository.isArchived(),
                null,
                null,
                repository.getLastActivityAt()
        );

        int seen = 0;
        int created = 0;
        int updated = 0;
        String cursor = null;

        do {
            PagedResult<ProviderContribution> page =
                    github.listContributions(token, providerRepository, since, cursor);

            for (ProviderContribution pc : page.items()) {
                Contribution.Type type = mapType(pc.type());

                Contribution contribution = contributions.findByProviderIdentity(
                        user.getId(),
                        "github",
                        pc.externalContributionId(),
                        type
                ).orElse(null);

                if (contribution == null) {
                    contribution = new Contribution(
                            user,
                            repository,
                            "github",
                            pc.externalContributionId(),
                            type,
                            pc.occurredAt()
                    );
                    contributions.persist(contribution);
                    created++;
                } else {
                    updated++;
                }

                contribution.updateFromDiscovery(
                        pc.title(),
                        pc.occurredAt(),
                        mapState(pc.state()),
                        pc.additions(),
                        pc.deletions(),
                        pc.changedFiles(),
                        pc.merged()
                );
                seen++;
            }

            cursor = page.nextCursor();
        } while (cursor != null);

        return new DiscoveryResult(repository.getId(), seen, created, updated);
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
        String owner = repository.getOwnerLogin();
        return owner == null
                ? ProviderRepository.OwnerType.OTHER
                : ProviderRepository.OwnerType.USER;
    }

    public record DiscoveryResult(UUID repositoryId, int seen, int created, int updated) {}
}
