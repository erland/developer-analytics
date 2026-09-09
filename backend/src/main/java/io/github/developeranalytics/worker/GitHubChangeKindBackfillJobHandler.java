package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.change.ContributionFileChange;
import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.ContributionFileChangeRepository;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderContributionFileChange;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.history.ContributionHistoryProvider;
import io.github.developeranalytics.provider.history.HistoricalCommitFileChanges;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import io.github.developeranalytics.service.discovery.GitHubCommitFileChangeService;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class GitHubChangeKindBackfillJobHandler implements BackgroundJobHandler {

    public static final String JOB_TYPE = "GITHUB_CHANGE_KIND_BACKFILL";
    static final int BATCH_SIZE = 100;

    @Inject SourceRepositoryRepository repositories;
    @Inject ContributionRepository contributions;
    @Inject ContributionFileChangeRepository fileChanges;
    @Inject ProviderCredentialService credentials;
    @Inject ContributionHistoryProvider contributionHistory;
    @Inject GitHubCommitFileChangeService commitFileChanges;
    @Inject ChangeKindClassifier classifier;
    @Inject RepositoryDiscoveryJobService jobs;

    @Override
    public String jobType() {
        return JOB_TYPE;
    }

    @Override
    @Transactional
    public void handle(BackgroundJob job) throws Exception {
        if (job.getUser() == null) {
            throw new IllegalStateException("Change-kind backfill job requires a user");
        }

        Object repositoryIdValue = job.getPayload().get("repositoryId");
        if (repositoryIdValue == null) {
            throw new IllegalStateException("Change-kind backfill job requires repositoryId");
        }

        UUID repositoryId = UUID.fromString(repositoryIdValue.toString());
        SourceRepository repository = repositories.findByIdForUser(repositoryId, job.getUser().getId())
                .orElseThrow(() -> new IllegalStateException("Repository not found for job user"));

        if (repository.getContributionScopeVersion() >= SourceRepository.CURRENT_CONTRIBUTION_SCOPE_VERSION) {
            return;
        }

        ProviderAccessToken token = credentials.requireAccessToken(job.getUser().getId(), "github");
        List<Contribution> batch = contributions.findCommitsMissingFileClassification(
                job.getUser().getId(), repositoryId, ChangeKindClassifier.CLASSIFIER_VERSION, BATCH_SIZE);

        Map<String, HistoricalCommitFileChanges> gitChanges = fetchGitChanges(repository, batch, token);
        for (Contribution contribution : batch) {
            HistoricalCommitFileChanges gitDetails = gitChanges.get(contribution.getProviderContributionId());
            if (isUsable(gitDetails)) {
                persistGitChanges(job, repository, contribution, gitDetails.fileChanges());
            } else {
                GitHubCommitFileChangeService.CommitDetails details =
                        commitFileChanges.refresh(job.getUser(), repository, contribution, token);
                contribution.updateFileStatistics(details.additions(), details.deletions(), details.changedFiles());
            }
        }

        boolean remaining = fileChanges.hasMissingCurrentClassification(
                job.getUser().getId(), repositoryId, ChangeKindClassifier.CLASSIFIER_VERSION);
        if (remaining) {
            jobs.enqueueChangeKindBackfillContinuation(job.getUser(), repositoryId, job.getId());
        } else {
            repository.markContributionScopeCurrent();
        }
    }

    private Map<String, HistoricalCommitFileChanges> fetchGitChanges(
            SourceRepository repository,
            List<Contribution> batch,
            ProviderAccessToken token
    ) {
        if (batch.isEmpty()) return Map.of();

        List<String> commitShas = batch.stream()
                .map(Contribution::getProviderContributionId)
                .toList();
        try {
            List<HistoricalCommitFileChanges> fetched = contributionHistory.fetchFileChanges(
                    token, toProviderRepository(repository), commitShas);
            Map<String, HistoricalCommitFileChanges> bySha = new HashMap<>();
            for (HistoricalCommitFileChanges change : fetched) {
                if (change != null && !bySha.containsKey(change.commitSha())) {
                    bySha.put(change.commitSha(), change);
                }
            }
            return bySha;
        } catch (ProviderException | RuntimeException ignored) {
            // Git is an optimization for historical enrichment. If clone/history inspection
            // fails, retain the existing per-commit REST path as the correctness fallback.
            return Map.of();
        }
    }

    private void persistGitChanges(
            BackgroundJob job,
            SourceRepository repository,
            Contribution contribution,
            List<ProviderContributionFileChange> changes
    ) {
        int additions = 0;
        int deletions = 0;

        // Replace only after the complete Git result for this commit has been obtained and
        // validated, matching the all-or-nothing behavior of the existing REST implementation.
        fileChanges.deleteForContribution(contribution);
        for (ProviderContributionFileChange change : changes) {
            fileChanges.persist(new ContributionFileChange(
                    contribution,
                    job.getUser(),
                    repository,
                    change.path(),
                    change.additions(),
                    change.deletions(),
                    classifier.classify(change.path()),
                    contribution.getOccurredAt()
            ));
            additions = saturatingAdd(additions, change.additions());
            deletions = saturatingAdd(deletions, change.deletions());
        }
        contribution.updateFileStatistics(additions, deletions, changes.size());
    }

    private static boolean isUsable(HistoricalCommitFileChanges details) {
        if (details == null || details.fileChanges() == null) return false;
        for (ProviderContributionFileChange change : details.fileChanges()) {
            if (change == null
                    || change.path() == null
                    || change.path().isBlank()
                    || change.additions() < 0
                    || change.deletions() < 0) {
                return false;
            }
        }
        return true;
    }

    private static int saturatingAdd(int current, int value) {
        long sum = (long) current + value;
        return (int) Math.min(Integer.MAX_VALUE, sum);
    }

    private static ProviderRepository toProviderRepository(SourceRepository repository) {
        ProviderRepository.Visibility visibility = ProviderRepository.Visibility.valueOf(repository.getVisibility().name());
        return new ProviderRepository(
                repository.getExternalRepositoryId(),
                repository.getOwnerExternalId(),
                repository.getOwnerLogin(),
                ProviderRepository.OwnerType.OTHER,
                repository.getName(),
                repository.getFullName(),
                repository.getHtmlUrl(),
                visibility,
                repository.isFork(),
                repository.isArchived(),
                null,
                null,
                repository.getLastActivityAt(),
                repository.getDescription(),
                repository.getTopics(),
                repository.getRepositorySizeBytes()
        );
    }
}
