package io.github.developeranalytics.service.sync;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.job.BackgroundJobStatus;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.ContributionSyncMode;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import io.github.developeranalytics.worker.GitHubChangeKindBackfillJobHandler;
import io.github.developeranalytics.worker.GitHubContributionDiscoveryJobHandler;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@Tag("persistence")
class ExistingChangeKindBackfillDeploymentTest {

    @Inject EntityManager entityManager;
    @Inject ContributionScopeUpgradeService upgrades;
    @Inject BackgroundJobRepository jobs;

    @Test
    void existingCurrentRepositoryIsAutomaticallyQueuedForResumableScopeBackfill() {
        UUID[] ids = new UUID[2];

        QuarkusTransaction.requiringNew().run(() -> {
            AppUser user = AppUser.create();
            entityManager.persist(user);

            SourceRepository repository = new SourceRepository(
                    user, "github", "existing-change-kind-gap", "owner", "existing-change-kind-gap");
            repository.markContributionScopeCurrent();
            repository.markSynced(OffsetDateTime.now(ZoneOffset.UTC));
            entityManager.persist(repository);

            Contribution commit = new Contribution(
                    user,
                    repository,
                    "github",
                    "already-imported-commit",
                    Contribution.Type.COMMIT,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
            // Existing production-style row: commit-level statistics are complete, but the
            // file-level Change Type classification has not been persisted yet.
            commit.updateFileStatistics(12, 4, 2);
            entityManager.persist(commit);
            entityManager.flush();

            ids[0] = user.getId();
            ids[1] = repository.getId();
        });

        int enqueued = upgrades.enqueueMissingBackfills();
        assertEquals(1, enqueued,
                "the periodic reconciliation pass must queue existing incomplete repositories without a full resync");

        QuarkusTransaction.requiringNew().run(() -> {
            List<BackgroundJob> active = jobs.findActiveForUser(ids[0]);
            assertEquals(1, active.size());

            BackgroundJob job = active.getFirst();
            assertEquals(GitHubChangeKindBackfillJobHandler.JOB_TYPE, job.getJobType());
            assertEquals(BackgroundJobStatus.QUEUED, job.getStatus());
            assertEquals(ids[1].toString(), job.getPayload().get("repositoryId"));
            assertEquals("github", job.getPayload().get("provider"));
            assertEquals(
                    ContributionSyncMode.SCOPE_BACKFILL.name(),
                    job.getPayload().get(GitHubContributionDiscoveryJobHandler.SYNC_MODE)
            );
            assertNotNull(job.getDeduplicationKey());
        });

        assertEquals(0, upgrades.enqueueMissingBackfills(),
                "a later reconciliation pass must rely on active-job deduplication rather than queueing duplicates");

        QuarkusTransaction.requiringNew().run(() -> {
            assertEquals(1, jobs.findActiveForUser(ids[0]).size(),
                    "only one resumable backfill job should remain active for the repository");
        });

        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                        "delete from app_user where id=:userId")
                .setParameter("userId", ids[0])
                .executeUpdate());
    }
}
