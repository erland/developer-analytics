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
    private static final int COLD_START_PRIORITY = -1_000;
    // Default reserve is max(200, 5% of 5,000) = 250. Start 50 requests above
    // the reserve so five repositories can complete at 10 simulated requests each.
    private static final int INITIAL_REMAINING = 300;
    private static final int REQUESTS_PER_REPOSITORY = 10;
    private static final int EXPECTED_COMPLETED_BEFORE_PAUSE = 5;

    @Inject EntityManager entityManager;
    @Inject BackgroundJobRepository jobs;
    @Inject GitHubRateLimitService rateLimits;

    @Test
    @TestTransaction
    void coldStartRunsUntilReservePausesRemainingJobsThenResumesAndCompletesAfterReset() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime resetAt = now.plusMinutes(15);
        ProviderAccessToken token = new ProviderAccessToken("cold-start-acceptance-token");

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
                    COLD_START_PRIORITY,
                    Map.of(
                            "provider", "github",
                            "repositoryId", repository.getId().toString(),
                            "syncMode", "INITIAL_FULL"
                    ),
                    5,
                    now,
                    "github:contributions:" + repository.getId()
            );
            entityManager.persist(job);
        }
        entityManager.flush();

        int remaining = INITIAL_REMAINING;
        rateLimits.update(token, state(remaining, resetAt, now));

        int completedBeforePause = 0;
        int paused = 0;
        while (completedBeforePause + paused < REPOSITORY_COUNT) {
            BackgroundJob job = jobs.claimNext("acceptance-worker", now.plusSeconds(1))
                    .orElseThrow(() -> new AssertionError("cold-start fixture unexpectedly ran out of claimable jobs"));
            assertEquals(user.getId(), job.getUser().getId());
            assertEquals(BackgroundJobStatus.RUNNING, job.getStatus());

            GitHubRateLimitService.GitHubRateLimitDecision decision = rateLimits.decision(token, now.plusSeconds(1));
            if (!decision.allowed()) {
                job.deferForRateLimit(decision.resumeAt());
                paused++;
                continue;
            }

            job.complete();
            completedBeforePause++;
            remaining -= REQUESTS_PER_REPOSITORY;
            rateLimits.update(token, state(remaining, resetAt, now.plusSeconds(completedBeforePause + 1L)));
        }
        entityManager.flush();

        assertEquals(EXPECTED_COMPLETED_BEFORE_PAUSE, completedBeforePause,
                "some repositories should complete before the reserve threshold is reached");
        assertEquals(REPOSITORY_COUNT - EXPECTED_COMPLETED_BEFORE_PAUSE, paused,
                "all remaining repositories should pause once the reserve is reached");

        Number completedRowsBeforeReset = countJobs(user, "COMPLETED");
        Number pausedRowsBeforeReset = countJobs(user, "PAUSED_RATE_LIMIT");
        Number failedRowsBeforeReset = countJobs(user, "FAILED");
        Number attemptsBeforeReset = (Number) entityManager.createNativeQuery(
                        "SELECT coalesce(sum(attempt_count),0) FROM background_job WHERE user_id=:userId")
                .setParameter("userId", user.getId())
                .getSingleResult();

        assertEquals(EXPECTED_COMPLETED_BEFORE_PAUSE, completedRowsBeforeReset.intValue());
        assertEquals(REPOSITORY_COUNT - EXPECTED_COMPLETED_BEFORE_PAUSE, pausedRowsBeforeReset.intValue());
        assertEquals(0, failedRowsBeforeReset.intValue());
        assertEquals(EXPECTED_COMPLETED_BEFORE_PAUSE, attemptsBeforeReset.intValue(),
                "rate-limit deferrals must not consume attempts");
        assertTrue(jobs.claimNext("acceptance-worker", resetAt.minusSeconds(1)).isEmpty(),
                "paused jobs must not be claimable before GitHub reset");

        OffsetDateTime resumedAt = resetAt.plusSeconds(1);
        rateLimits.update(token, state(5_000, resumedAt.plusHours(1), resumedAt));

        int resumedAndCompleted = 0;
        while (true) {
            Optional<BackgroundJob> claimed = jobs.claimNext("acceptance-worker", resumedAt);
            if (claimed.isEmpty()) break;

            BackgroundJob job = claimed.get();
            assertEquals(user.getId(), job.getUser().getId());
            assertEquals(BackgroundJobStatus.RUNNING, job.getStatus());
            assertTrue(rateLimits.decision(token, resumedAt).allowed());
            assertNull(job.getLastError());

            job.complete();
            resumedAndCompleted++;
        }
        entityManager.flush();

        assertEquals(REPOSITORY_COUNT - EXPECTED_COMPLETED_BEFORE_PAUSE, resumedAndCompleted);
        assertEquals(REPOSITORY_COUNT, countJobs(user, "COMPLETED").intValue());
        assertEquals(0, countJobs(user, "PAUSED_RATE_LIMIT").intValue());
        assertEquals(0, countJobs(user, "FAILED").intValue());
        assertEquals(REPOSITORY_COUNT, countJobs(user, null).intValue(),
                "every repository cold-start job should eventually complete");

        rateLimits.clear(token);
    }

    private GitHubRateLimitState state(int remaining, OffsetDateTime resetAt, OffsetDateTime observedAt) {
        return new GitHubRateLimitState(
                5_000,
                remaining,
                resetAt,
                observedAt,
                "core",
                null,
                false
        );
    }

    private Number countJobs(AppUser user, String status) {
        if (status == null) {
            return (Number) entityManager.createNativeQuery(
                            "SELECT count(*) FROM background_job WHERE user_id=:userId")
                    .setParameter("userId", user.getId())
                    .getSingleResult();
        }
        return (Number) entityManager.createNativeQuery(
                        "SELECT count(*) FROM background_job WHERE user_id=:userId AND status=:status")
                .setParameter("userId", user.getId())
                .setParameter("status", status)
                .getSingleResult();
    }
}
