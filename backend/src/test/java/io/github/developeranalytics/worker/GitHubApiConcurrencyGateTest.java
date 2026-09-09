package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("worker-job")
class GitHubApiConcurrencyGateTest {

    @Test
    void gatesOnlyRestHeavyGitHubJobs() {
        assertTrue(GitHubApiConcurrencyGate.isApiHeavyJobType(GitHubRepositoryDiscoveryJobHandler.JOB_TYPE));
        assertTrue(GitHubApiConcurrencyGate.isApiHeavyJobType(GitHubContributionDiscoveryJobHandler.JOB_TYPE));
        assertTrue(GitHubApiConcurrencyGate.isApiHeavyJobType(GitHubLanguageEvidenceJobHandler.JOB_TYPE));
        assertFalse(GitHubApiConcurrencyGate.isApiHeavyJobType(GitHubChangeKindBackfillJobHandler.JOB_TYPE));
        assertFalse(GitHubApiConcurrencyGate.isApiHeavyJobType(GitHubFileManifestEvidenceJobHandler.JOB_TYPE));
    }

    @Test
    void blocksWhenPerUserLimitIsAlreadyOccupied() throws Exception {
        UUID userId = UUID.randomUUID();
        AppUser user = userWithId(userId);
        BackgroundJob job = BackgroundJob.queued(
                user,
                GitHubContributionDiscoveryJobHandler.JOB_TYPE,
                100,
                Map.of(),
                5,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        GitHubApiConcurrencyGate gate = new GitHubApiConcurrencyGate();
        gate.jobs = repositoryReturning(2L, userId);
        gate.maxConcurrentJobsPerUser = 2;
        gate.deferSeconds = 7;
        OffsetDateTime now = OffsetDateTime.of(2026, 9, 9, 15, 0, 0, 0, ZoneOffset.UTC);

        GitHubApiConcurrencyGate.Decision decision = gate.decision(job, now);

        assertFalse(decision.allowed());
        assertEquals(2L, decision.running());
        assertEquals(2, decision.limit());
        assertEquals(now.plusSeconds(7), decision.retryAt());
    }

    @Test
    void allowsWhenThereIsCapacity() throws Exception {
        UUID userId = UUID.randomUUID();
        BackgroundJob job = BackgroundJob.queued(
                userWithId(userId),
                GitHubLanguageEvidenceJobHandler.JOB_TYPE,
                100,
                Map.of(),
                5,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        GitHubApiConcurrencyGate gate = new GitHubApiConcurrencyGate();
        gate.jobs = repositoryReturning(1L, userId);
        gate.maxConcurrentJobsPerUser = 2;
        gate.deferSeconds = 5;

        assertTrue(gate.decision(job, OffsetDateTime.now(ZoneOffset.UTC)).allowed());
    }

    private static BackgroundJobRepository repositoryReturning(long count, UUID expectedUserId) {
        return new BackgroundJobRepository() {
            @Override
            public long countRunningJobsForUserByTypesExcept(
                    UUID userId,
                    Collection<String> jobTypes,
                    UUID excludedJobId
            ) {
                assertEquals(expectedUserId, userId);
                assertFalse(jobTypes.isEmpty());
                return count;
            }
        };
    }

    private static AppUser userWithId(UUID id) throws Exception {
        AppUser user = AppUser.create();
        Field field = AppUser.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
        return user;
    }
}
