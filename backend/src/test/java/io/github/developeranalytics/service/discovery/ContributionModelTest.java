package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.*;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ContributionModelTest {

    @Test
    void updatesDiscoveredContribution() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(
                user, "github", "repo-1", "alice", "sample");

        Contribution contribution = new Contribution(
                user,
                repository,
                "github",
                "abc123",
                Contribution.Type.COMMIT,
                OffsetDateTime.parse("2026-08-30T08:00:00Z")
        );

        contribution.updateFromDiscovery(
                "Initial implementation",
                OffsetDateTime.parse("2026-08-30T08:00:00Z"),
                Contribution.State.UNKNOWN,
                10,
                2,
                3,
                null
        );

        assertEquals("abc123", contribution.getProviderContributionId());
        assertEquals(Contribution.Type.COMMIT, contribution.getType());
        assertEquals("Initial implementation", contribution.getTitle());
    }


    @Test
    void supportsIssueAndReviewTypes() {
        assertEquals(Contribution.Type.ISSUE, Contribution.Type.valueOf("ISSUE"));
        assertEquals(Contribution.Type.REVIEW, Contribution.Type.valueOf("REVIEW"));
    }
}
