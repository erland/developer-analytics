package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.ProviderSyncRun;
import io.github.developeranalytics.service.discovery.GitHubRepositoryDiscoveryService;
import io.github.developeranalytics.service.sync.ProviderSyncRunService;
import io.github.developeranalytics.service.sync.RepositoryAnalysisOrchestrator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GitHubRepositoryDiscoveryJobHandler implements BackgroundJobHandler {

    public static final String JOB_TYPE = "GITHUB_REPOSITORY_DISCOVERY";

    @Inject GitHubRepositoryDiscoveryService discovery;
    @Inject RepositoryAnalysisOrchestrator analysis;
    @Inject ProviderSyncRunService providerSyncRuns;

    @Override public String jobType() { return JOB_TYPE; }

    @Override
    public void handle(BackgroundJob job) throws Exception {
        if (job.getUser() == null) throw new IllegalStateException("Repository discovery job requires a user");
        discovery.discover(job.getUser());

        ProviderSyncRun run = providerSyncRuns.start(job.getUser(), "github");
        RepositoryAnalysisOrchestrator.QueueResult queued = analysis.enqueueAll(job.getUser(), run.getId());
        providerSyncRuns.plan(run.getId(), queued.contributionJobsQueued());
        job.putPayloadValue(ProviderSyncRunService.PAYLOAD_KEY, run.getId().toString());
    }
}
