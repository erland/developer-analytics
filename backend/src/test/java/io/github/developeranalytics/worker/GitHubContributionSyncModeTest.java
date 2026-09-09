package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.model.ContributionSyncMode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class GitHubContributionSyncModeTest {

    @Test
    void noSinceMeansInitialFullSync() {
        assertEquals(
                ContributionSyncMode.INITIAL_FULL,
                GitHubContributionDiscoveryJobHandler.determineSyncMode(null)
        );
    }

    @Test
    void sinceMeansIncrementalSync() {
        OffsetDateTime since = OffsetDateTime.of(2026, 9, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        assertEquals(
                ContributionSyncMode.INCREMENTAL,
                GitHubContributionDiscoveryJobHandler.determineSyncMode(since)
        );
    }
}
