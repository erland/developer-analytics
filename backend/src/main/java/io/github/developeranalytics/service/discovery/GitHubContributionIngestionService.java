package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderContribution;
import io.github.developeranalytics.provider.ProviderException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Persists and enriches one GitHub contribution discovered by the provider adapter. */
@ApplicationScoped
public class GitHubContributionIngestionService {

    @Inject ContributionRepository contributions;
    @Inject GitHubCommitFileChangeService commitFileChanges;

    public IngestionResult ingest(
            AppUser user,
            SourceRepository repository,
            ProviderContribution providerContribution,
            ProviderAccessToken token
    ) throws ProviderException {
        if (user == null) throw new IllegalArgumentException("user is required");
        if (repository == null) throw new IllegalArgumentException("repository is required");
        if (providerContribution == null) throw new IllegalArgumentException("providerContribution is required");

        Contribution.Type type = mapType(providerContribution.type());
        Contribution contribution = contributions.findByProviderIdentity(
                user.getId(), "github", providerContribution.externalContributionId(), type).orElse(null);
        boolean existing = contribution != null;

        if (!existing) {
            contribution = new Contribution(
                    user,
                    repository,
                    "github",
                    providerContribution.externalContributionId(),
                    type,
                    providerContribution.occurredAt()
            );
            contributions.persist(contribution);
        }

        boolean cachedCommitDetails = type == Contribution.Type.COMMIT
                && existing
                && commitFileChanges.hasCurrentClassification(contribution);

        contribution.updateFromDiscovery(
                providerContribution.title(),
                providerContribution.occurredAt(),
                mapState(providerContribution.state()),
                cachedCommitDetails ? contribution.getAdditions() : providerContribution.additions(),
                cachedCommitDetails ? contribution.getDeletions() : providerContribution.deletions(),
                cachedCommitDetails ? contribution.getChangedFiles() : providerContribution.changedFiles(),
                providerContribution.merged()
        );

        if (type == Contribution.Type.COMMIT && !cachedCommitDetails) {
            GitHubCommitFileChangeService.CommitDetails details =
                    commitFileChanges.refresh(user, repository, contribution, token);
            contribution.updateFileStatistics(
                    details.additions(), details.deletions(), details.changedFiles());
        }

        return new IngestionResult(!existing, existing);
    }

    static Contribution.Type mapType(ProviderContribution.Type type) {
        return switch (type) {
            case COMMIT -> Contribution.Type.COMMIT;
            case PULL_REQUEST -> Contribution.Type.PULL_REQUEST;
            case REVIEW -> Contribution.Type.REVIEW;
            case ISSUE -> Contribution.Type.ISSUE;
        };
    }

    static Contribution.State mapState(ProviderContribution.State state) {
        return switch (state) {
            case OPEN -> Contribution.State.OPEN;
            case CLOSED -> Contribution.State.CLOSED;
            case MERGED -> Contribution.State.MERGED;
            case UNKNOWN -> Contribution.State.UNKNOWN;
        };
    }

    public record IngestionResult(boolean created, boolean updated) {}
}
