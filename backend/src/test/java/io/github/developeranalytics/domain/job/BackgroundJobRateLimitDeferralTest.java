package io.github.developeranalytics.domain.job;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class BackgroundJobRateLimitDeferralTest {

    @Test
    void deferringClaimedJobDoesNotConsumeAnAttempt() {
        OffsetDateTime now = OffsetDateTime.of(2026, 9, 9, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime resumeAt = now.plusMinutes(30);
        BackgroundJob job = BackgroundJob.queued(
                null,
                "GITHUB_CONTRIBUTION_DISCOVERY",
                100,
                Map.of(),
                5,
                now
        );

        job.markRunning("worker-1", now);
        assertEquals(1, job.getAttemptCount());
        assertEquals(BackgroundJobStatus.RUNNING, job.getStatus());

        job.deferWithoutAttempt(resumeAt);

        assertEquals(0, job.getAttemptCount());
        assertEquals(BackgroundJobStatus.WAITING, job.getStatus());
        assertEquals(resumeAt, job.getNextExecutionAt());
        assertNull(job.getLockedAt());
        assertNull(job.getLastError());
    }

    @Test
    void onlyRunningJobsCanBeDeferred() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        BackgroundJob job = BackgroundJob.queued(null, "TEST", 100, Map.of(), 5, now);

        assertThrows(IllegalStateException.class, () -> job.deferWithoutAttempt(now.plusMinutes(1)));
    }
}
