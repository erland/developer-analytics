package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.ProviderSyncRun;
import io.github.developeranalytics.persistence.repository.ProviderSyncRunRepository;
import io.github.developeranalytics.service.discovery.GitHubRepositoryDiscoveryService;
import io.github.developeranalytics.service.sync.ProviderSyncRunService;
import io.github.developeranalytics.service.sync.RepositoryAnalysisOrchestrator;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class GitHubRepositoryDiscoveryJobHandler implements BackgroundJobHandler {

    public static final String JOB_TYPE = "GITHUB_REPOSITORY_DISCOVERY";

    @Inject GitHubRepositoryDiscoveryService discovery;
    @Inject RepositoryAnalysisOrchestrator analysis;
    @Inject ProviderSyncRunService providerSyncRuns;
    @Inject ProviderSyncRunRepository providerSyncRunRepository;

    @Override public String jobType() { return JOB_TYPE; }

    @Override
    public void handle(BackgroundJob job) throws Exception {
        if (job.getUser() == null) throw new IllegalStateException("Repository discovery job requires a user");

        UUID runId = providerSyncRuns.payloadRunId(job);
        ProviderSyncRun run = runId == null ? null : providerSyncRunRepository.findById(runId).orElse(null);
        if (run == null) {
            run = providerSyncRuns.start(job.getUser(), "github");
            runId = run.getId();
            job.putPayloadValue(ProviderSyncRunService.PAYLOAD_KEY, runId.toString());
        }

        discovery.discover(job.getUser());
        RepositoryAnalysisOrchestrator.QueueResult queued = analysis.enqueueAll(job.getUser(), runId);
        providerSyncRuns.plan(runId, queued.contributionJobsQueued());
    }
}
