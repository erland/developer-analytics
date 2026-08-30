package io.github.developeranalytics.domain.model;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class RepositorySyncRunTest {

    @Test
    void tracksProgressAndCompletion() {
        AppUser user = AppUser.create();
        RepositorySyncRun run = new RepositorySyncRun(user, "github");

        OffsetDateTime started = OffsetDateTime.parse("2026-08-30T08:00:00Z");
        OffsetDateTime reset = OffsetDateTime.parse("2026-08-30T09:00:00Z");

        run.start(started);
        run.progress(120, 100, 20, 2, 4875, reset);

        assertEquals(RepositorySyncRun.Status.RUNNING, run.getStatus());
        assertEquals(120, run.getRepositoriesSeen());
        assertEquals(2, run.getPagesProcessed());
        assertEquals(4875, run.getRateLimitRemaining());
        assertEquals(reset, run.getRateLimitResetAt());

        run.complete(started.plusMinutes(1));

        assertEquals(RepositorySyncRun.Status.COMPLETED, run.getStatus());
        assertNotNull(run.getCompletedAt());
    }

    @Test
    void tracksRateLimitedFailure() {
        AppUser user = AppUser.create();
        RepositorySyncRun run = new RepositorySyncRun(user, "github");

        OffsetDateTime now = OffsetDateTime.parse("2026-08-30T08:00:00Z");
        OffsetDateTime reset = now.plusMinutes(30);

        run.start(now);
        run.rateLimited("HTTP 429", reset, now.plusSeconds(5));

        assertEquals(RepositorySyncRun.Status.RATE_LIMITED, run.getStatus());
        assertEquals("HTTP 429", run.getLastError());
        assertEquals(reset, run.getRateLimitResetAt());
    }
}
