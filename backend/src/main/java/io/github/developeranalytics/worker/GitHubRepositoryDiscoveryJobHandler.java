package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.service.discovery.GitHubRepositoryDiscoveryService;
import io.github.developeranalytics.service.sync.ContributionSyncOrchestrator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitHubRepositoryDiscoveryJobHandler implements BackgroundJobHandler {

    public static final String JOB_TYPE = "GITHUB_REPOSITORY_DISCOVERY";

    @Inject
    GitHubRepositoryDiscoveryService discovery;

    @Inject
    ContributionSyncOrchestrator contributionSync;

    @Override
    public String jobType() {
        return JOB_TYPE;
    }

    @Override
    public void handle(BackgroundJob job) throws Exception {
        if (job.getUser() == null) {
            throw new IllegalStateException("Repository discovery job requires a user");
        }
        discovery.discover(job.getUser());

        // Seed the first contribution batch after repository inventory refresh.
        contributionSync.enqueueBatch(
                job.getUser(),
                0,
                ContributionSyncOrchestrator.DEFAULT_BATCH_SIZE
        );
    }
}
