package io.github.developeranalytics.provider.github;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.job.BackgroundJobStatus;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.worker.GitHubContributionDiscoveryJobHandler;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Tag("persistence")
class GitHubColdStartRateLimitAcceptanceTest {

    private static final int REPOSITORY_COUNT = 240;

    @Inject EntityManager entityManager;
    @Inject BackgroundJobRepository jobs;
    @Inject GitHubRateLimitService rateLimits;

    @Test
    @TestTransaction
    void coldStartPausesHundredsOfJobsWithoutFailuresAndResumesAfterReset() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime resetAt = now.plusMinutes(15);
        ProviderAccessToken token = new ProviderAccessToken("cold-start-acceptance-token");

        rateLimits.update(token, new GitHubRateLimitState(
                5_000,
                100,
                resetAt,
                now,
                "core",
                null,
                false
        ));
        GitHubRateLimitService.GitHubRateLimitDecision decision = rateLimits.decision(token);
        assertFalse(decision.allowed());
        assertEquals(resetAt, decision.resumeAt());

        AppUser user = AppUser.create();
        entityManager.persist(user);
        entityManager.flush();

        for (int index = 1; index <= REPOSITORY_COUNT; index++) {
            SourceRepository repository = new SourceRepository(
                    user,
                    "github",
                    "cold-start-" + index,
                    "large-account",
                    "repository-" + index
            );
            entityManager.persist(repository);
            entityManager.flush();

            BackgroundJob job = BackgroundJob.queuedDeduplicated(
                    user,
                    GitHubContributionDiscoveryJobHandler.JOB_TYPE,
                    110,
                    Map.of(
                            "provider", "github",
                            "repositoryId", repository.getId().toString(),
                            "syncMode", "INITIAL_FULL"
                    ),
                    5,
                    now,
                    "github:contributions:" + repository.getId()
            );
            job.markRunning("acceptance-worker", now);
            job.deferForRateLimit(decision.resumeAt());
            entityManager.persist(job);
        }
        entityManager.flush();

        Number paused = (Number) entityManager.createNativeQuery(
                        "SELECT count(*) FROM background_job WHERE user_id=:userId AND status='PAUSED_RATE_LIMIT'")
                .setParameter("userId", user.getId())
                .getSingleResult();
        Number failed = (Number) entityManager.createNativeQuery(
                        "SELECT count(*) FROM background_job WHERE user_id=:userId AND status='FAILED'")
                .setParameter("userId", user.getId())
                .getSingleResult();
        Number attempts = (Number) entityManager.createNativeQuery(
                        "SELECT coalesce(sum(attempt_count),0) FROM background_job WHERE user_id=:userId")
                .setParameter("userId", user.getId())
                .getSingleResult();

        assertEquals(REPOSITORY_COUNT, paused.intValue());
        assertEquals(0, failed.intValue());
        assertEquals(0, attempts.intValue());
        assertTrue(jobs.claimNext("acceptance-worker", now.plusMinutes(5)).isEmpty(),
                "paused jobs must not be claimable before GitHub reset");

        Optional<BackgroundJob> resumed = jobs.claimNext("acceptance-worker", resetAt.plusSeconds(1));
        assertTrue(resumed.isPresent(), "a paused cold-start job should become claimable after reset");
        assertEquals(BackgroundJobStatus.RUNNING, resumed.get().getStatus());
        assertEquals(1, resumed.get().getAttemptCount());
        assertNull(resumed.get().getLastError());

        Number remainingFailed = (Number) entityManager.createNativeQuery(
                        "SELECT count(*) FROM background_job WHERE user_id=:userId AND status='FAILED'")
                .setParameter("userId", user.getId())
                .getSingleResult();
        assertEquals(0, remainingFailed.intValue());

        rateLimits.clear(token);
    }
}
