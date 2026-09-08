package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.ContributionFileChangeRepository;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import io.github.developeranalytics.service.discovery.GitHubCommitFileChangeService;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GitHubChangeKindBackfillJobHandler implements BackgroundJobHandler {

    public static final String JOB_TYPE = "GITHUB_CHANGE_KIND_BACKFILL";
    static final int BATCH_SIZE = 100;

    @Inject SourceRepositoryRepository repositories;
    @Inject ContributionRepository contributions;
    @Inject ContributionFileChangeRepository fileChanges;
    @Inject ProviderCredentialService credentials;
    @Inject GitHubCommitFileChangeService commitFileChanges;
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

        for (Contribution contribution : batch) {
            GitHubCommitFileChangeService.CommitDetails details =
                    commitFileChanges.refresh(job.getUser(), repository, contribution, token);
            contribution.updateFileStatistics(details.additions(), details.deletions(), details.changedFiles());
        }

        boolean remaining = fileChanges.hasMissingCurrentClassification(
                job.getUser().getId(), repositoryId, ChangeKindClassifier.CLASSIFIER_VERSION);
        if (remaining) {
            jobs.enqueueChangeKindBackfillContinuation(job.getUser(), repositoryId);
        } else {
            repository.markContributionScopeCurrent();
        }
    }
}
