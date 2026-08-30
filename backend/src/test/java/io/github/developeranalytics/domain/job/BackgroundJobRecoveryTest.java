package io.github.developeranalytics.domain.job;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BackgroundJobRecoveryTest {

    @Test
    void interruptedRunningJobCanBeRecoveredAndRetried() {
        BackgroundJob job = BackgroundJob.queued(
                null,
                "TEST",
                100,
                Map.of(),
                5,
                OffsetDateTime.now()
        );

        OffsetDateTime started = OffsetDateTime.now();
        job.markRunning("worker-a", started);
        assertEquals(BackgroundJobStatus.RUNNING, job.getStatus());

        OffsetDateTime retryAt = started.plusMinutes(10);
        job.recoverInterrupted(retryAt);

        assertEquals(BackgroundJobStatus.WAITING, job.getStatus());
        assertNull(job.getLockedAt());
    }

    @Test
    void repeatedFailuresEventuallyBecomeTerminal() {
        BackgroundJob job = BackgroundJob.queued(
                null,
                "TEST",
                100,
                Map.of(),
                2,
                OffsetDateTime.now()
        );

        job.markRunning("worker-a", OffsetDateTime.now());
        job.retryOrFail("first", OffsetDateTime.now());
        assertEquals(BackgroundJobStatus.WAITING, job.getStatus());

        job.markRunning("worker-b", OffsetDateTime.now());
        job.retryOrFail("second", OffsetDateTime.now());
        assertEquals(BackgroundJobStatus.FAILED, job.getStatus());
    }
}
