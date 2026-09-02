package io.github.developeranalytics.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@Tag("privacy")
@Tag("unit")
class SourceRepositorySelectionTest {
    @Test
    void repositoryExposedByGitHubAppIsIncludedAutomatically() {
        SourceRepository repository = new SourceRepository(
                AppUser.create(), "github", "1", "alice", "private-app");

        // Simulate stale state from the previous local-selection model.
        repository.excludeFromAnalysis();
        assertFalse(repository.isIncludedInAnalysis());

        repository.updateFromDiscovery(
                "1", "alice", "private-app", "alice/private-app", null, null,
                List.of(), RepositoryOwnerType.USER,
                RepositoryOwnershipRelation.OWNED_BY_USER,
                RepositoryVisibility.PRIVATE, false, false,
                OffsetDateTime.now(), OffsetDateTime.now());

        // GitHub App installation scope is now the source of truth. Rediscovery
        // re-includes repositories that GitHub exposes to the application.
        assertTrue(repository.isIncludedInAnalysis());
    }
}
