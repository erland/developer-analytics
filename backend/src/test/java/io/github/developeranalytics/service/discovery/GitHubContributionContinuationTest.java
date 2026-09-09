package io.github.developeranalytics.service.discovery;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class GitHubContributionContinuationTest {

    @Test
    void contributorStatsContinuationIsDistinctFromCommitPageCursor() {
        assertTrue(GitHubContributionDiscoveryService.isContributorStatsContinuation(
                GitHubContributionDiscoveryService.CONTRIBUTOR_STATS_CONTINUATION));
        assertFalse(GitHubContributionDiscoveryService.isContributorStatsContinuation(null));
        assertFalse(GitHubContributionDiscoveryService.isContributorStatsContinuation("2"));
    }
}
