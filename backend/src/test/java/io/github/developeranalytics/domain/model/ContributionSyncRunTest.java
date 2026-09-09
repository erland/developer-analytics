package io.github.developeranalytics.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ContributionSyncRunTest {
    @Test
    void tracksProgressAndCompletion() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(
                user, "github", "repo-1", "alice", "demo");
        ContributionSyncRun run = new ContributionSyncRun(user, repository, "github");

        OffsetDateTime started = OffsetDateTime.parse("2026-08-30T08:00:00Z");
        OffsetDateTime reset = OffsetDateTime.parse("2026-08-30T09:00:00Z");

        run.start(started);
        run.progress(250, 200, 50, 3, 4700, reset);

        assertEquals(ContributionSyncRun.Status.RUNNING, run.getStatus());
        assertEquals(250, run.getContributionsSeen());
        assertEquals(3, run.getPagesProcessed());
        assertEquals(4700, run.getRateLimitRemaining());

        run.complete(started.plusMinutes(1));
        assertEquals(ContributionSyncRun.Status.COMPLETED, run.getStatus());
        assertNotNull(run.getCompletedAt());
    }

    @Test
    void tracksRateLimitedRun() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(
                user, "github", "repo-1", "alice", "demo");
        ContributionSyncRun run = new ContributionSyncRun(user, repository, "github");

        OffsetDateTime now = OffsetDateTime.parse("2026-08-30T08:00:00Z");
        OffsetDateTime reset = now.plusMinutes(30);

        run.start(now);
        run.rateLimited("HTTP 429", reset, now.plusSeconds(10));

        assertEquals(ContributionSyncRun.Status.RATE_LIMITED, run.getStatus());
        assertEquals("HTTP 429", run.getLastError());
        assertEquals(reset, run.getRateLimitResetAt());
    }

    @Test
    void tracksApiUsageWithDefensiveEndpointSnapshot() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(
                user, "github", "repo-1", "alice", "demo");
        ContributionSyncRun run = new ContributionSyncRun(
                user, repository, "github", ContributionSyncMode.INITIAL_FULL);

        Map<String, Integer> endpoints = new LinkedHashMap<>();
        endpoints.put("commits", 4);
        endpoints.put("issues", 1);
        run.apiUsage(5, endpoints);
        endpoints.put("reviews", 99);

        assertEquals(5, run.getApiRequestCount());
        assertEquals(Map.of("commits", 4, "issues", 1), run.getApiRequestsByEndpoint());
        assertThrows(UnsupportedOperationException.class,
                () -> run.getApiRequestsByEndpoint().put("languages", 1));
    }
}
