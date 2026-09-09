package io.github.developeranalytics.provider.github;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class GitHubApiUsageTrackerTest {

    @Test
    void countsRequestsByEndpointWithinScope() {
        GitHubApiUsageTracker tracker = new GitHubApiUsageTracker();

        try (GitHubApiUsageTracker.Scope scope = tracker.begin()) {
            tracker.record("commits");
            tracker.record("commits");
            tracker.record("issues");

            GitHubApiUsageTracker.UsageSnapshot snapshot = scope.snapshot();
            assertEquals(3, snapshot.totalRequests());
            assertEquals(2, snapshot.requestsByEndpoint().get("commits"));
            assertEquals(1, snapshot.requestsByEndpoint().get("issues"));
        }

        assertEquals(0, tracker.snapshot().totalRequests());
    }

    @Test
    void nestedScopeDoesNotContaminateOuterScope() {
        GitHubApiUsageTracker tracker = new GitHubApiUsageTracker();

        try (GitHubApiUsageTracker.Scope outer = tracker.begin()) {
            tracker.record("commits");
            try (GitHubApiUsageTracker.Scope inner = tracker.begin()) {
                tracker.record("languages");
                assertEquals(1, inner.snapshot().totalRequests());
            }
            tracker.record("issues");
            assertEquals(2, outer.snapshot().totalRequests());
        }
    }
}
