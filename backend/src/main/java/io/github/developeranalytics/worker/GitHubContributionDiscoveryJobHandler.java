package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.ContributionSyncMode;
import io.github.developeranalytics.domain.model.ProviderSyncRun;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.persistence.repository.ProviderSyncRunRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.service.discovery.GitHubContributionDiscoveryService;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import io.github.developeranalytics.service.sync.ProviderSyncRunService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.UUID;

@ApplicationScoped
public class GitHubContributionDiscoveryJobHandler implements BackgroundJobHandler {

    public static final String JOB_TYPE = "GITHUB_CONTRIBUTION_DISCOVERY";
    public static final String CONTINUATION_CURSOR = "contributionCursor";
    public static final String SYNC_MODE = "syncMode";
    public static final String FORCE_FULL_SYNC = "forceFullSync";
    private static final int INCREMENTAL_OVERLAP_DAYS = 3;

    @Inject SourceRepositoryRepository repositories;
    @Inject ContributionRepository contributions;
    @Inject GitHubContributionDiscoveryService discovery;
    @Inject RepositoryDiscoveryJobService jobs;
    @Inject ProviderSyncRunRepository providerSyncRunRepository;
    @Inject ProviderSyncRunService providerSyncRuns;

    @Override public String jobType() { return JOB_TYPE; }

    @Override
    public void handle(BackgroundJob job) throws Exception {
        if (job.getUser() == null) throw new IllegalStateException("Contribution discovery job requires a user");
        Object repositoryId = job.getPayload().get("repositoryId");
        if (repositoryId == null) throw new IllegalStateException("Contribution discovery job requires repositoryId");

        SourceRepository repository = repositories.findByIdForUser(UUID.fromString(repositoryId.toString()), job.getUser().getId())
                .orElseThrow(() -> new IllegalStateException("Repository not found for job user"));
        boolean forceFullSync = Boolean.parseBoolean(payloadString(job, FORCE_FULL_SYNC));
        OffsetDateTime since = forceFullSync ? null : repository.getContributionScopeVersion() < 2 ? null :
                contributions.latestCommitAt(job.getUser().getId(), repository.getId())
                        .map(latest -> latest.minusDays(INCREMENTAL_OVERLAP_DAYS)).orElse(null);

        ContributionSyncMode syncMode = determineSyncMode(since);
        job.putPayloadValue(SYNC_MODE, syncMode.name());
        UUID providerSyncRunId = providerSyncRuns.payloadRunId(job);
        ProviderSyncRun providerSyncRun = providerSyncRunId == null ? null : providerSyncRunRepository.findById(providerSyncRunId).orElse(null);
        providerSyncRuns.observeMode(providerSyncRunId, syncMode);

        String initialCursor = payloadString(job, CONTINUATION_CURSOR);
        GitHubContributionDiscoveryService.DiscoveryResult result = discovery.discover(
                job.getUser(), repository, since, initialCursor,
                cursor -> job.putPayloadValue(CONTINUATION_CURSOR, cursor), syncMode, providerSyncRun, forceFullSync);

        if (!result.complete()) {
            job.putPayloadValue(CONTINUATION_CURSOR, result.nextCursor());
            job.deferForRateLimit(result.resumeAt());
            return;
        }

        job.putPayloadValue(CONTINUATION_CURSOR, null);
        if (repository.getContributionScopeVersion() < SourceRepository.CURRENT_CONTRIBUTION_SCOPE_VERSION) {
            jobs.enqueueChangeKindBackfill(job.getUser(), repository.getId());
        }
    }

    static ContributionSyncMode determineSyncMode(OffsetDateTime since) {
        return since == null ? ContributionSyncMode.INITIAL_FULL : ContributionSyncMode.INCREMENTAL;
    }

    private String payloadString(BackgroundJob job, String key) {
        Object value = job.getPayload() == null ? null : job.getPayload().get(key);
        if (value == null) return null;
        String text = value.toString();
        return text.isBlank() ? null : text;
    }
}
