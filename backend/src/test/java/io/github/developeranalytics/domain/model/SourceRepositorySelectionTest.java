package io.github.developeranalytics.domain.model;

import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SourceRepositorySelectionTest {
    @Test
    void newlyDiscoveredPrivateRepositoryRequiresExplicitSelection() {
        SourceRepository repository = new SourceRepository(
                AppUser.create(), "github", "1", "alice", "private-app");
        repository.updateFromDiscovery(
                "1", "alice", "private-app", "alice/private-app", null, null,
                List.of(), RepositoryOwnerType.USER,
                RepositoryOwnershipRelation.OWNED_BY_USER,
                RepositoryVisibility.PRIVATE, false, false,
                OffsetDateTime.now(), OffsetDateTime.now());
        assertFalse(repository.isIncludedInAnalysis());
        repository.includeInAnalysis();
        assertTrue(repository.isIncludedInAnalysis());
        repository.excludeFromAnalysis();
        assertFalse(repository.isIncludedInAnalysis());
    }
}
