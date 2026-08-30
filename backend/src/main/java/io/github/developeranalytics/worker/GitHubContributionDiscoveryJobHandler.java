package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.service.discovery.GitHubContributionDiscoveryService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.UUID;

@ApplicationScoped
public class GitHubContributionDiscoveryJobHandler implements BackgroundJobHandler {

    public static final String JOB_TYPE = "GITHUB_CONTRIBUTION_DISCOVERY";

    @Inject
    SourceRepositoryRepository repositories;

    @Inject
    GitHubContributionDiscoveryService discovery;

    @Override
    public String jobType() {
        return JOB_TYPE;
    }

    @Override
    public void handle(BackgroundJob job) throws Exception {
        if (job.getUser() == null) {
            throw new IllegalStateException("Contribution discovery job requires a user");
        }

        Object repositoryId = job.getPayload().get("repositoryId");
        if (repositoryId == null) {
            throw new IllegalStateException("Contribution discovery job requires repositoryId");
        }

        SourceRepository repository = repositories.findByIdForUser(
                UUID.fromString(repositoryId.toString()),
                job.getUser().getId()
        ).orElseThrow(() -> new IllegalStateException("Repository not found for job user"));

        OffsetDateTime since = repository.getLastActivityAt() == null
                ? null
                : repository.getLastActivityAt().minusDays(30);

        discovery.discover(job.getUser(), repository, since);
    }
}
