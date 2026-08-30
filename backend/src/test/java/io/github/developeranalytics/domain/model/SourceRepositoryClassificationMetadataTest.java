package io.github.developeranalytics.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class SourceRepositoryClassificationMetadataTest {
    @Test
    void storesTopicsAndDescriptionFromProviderMetadata() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(
                user, "github", "repo-1", "alice", "demo-api");

        repository.updateFromDiscovery(
                "1", "alice", "demo-api", "alice/demo-api",
                "https://github.com/alice/demo-api",
                "Example REST API",
                List.of("api", "quarkus"),
                RepositoryOwnerType.USER,
                RepositoryOwnershipRelation.OWNED_BY_USER,
                RepositoryVisibility.PUBLIC,
                false, false,
                OffsetDateTime.parse("2026-08-30T08:00:00Z"),
                OffsetDateTime.parse("2026-08-30T08:00:00Z")
        );

        assertEquals("Example REST API", repository.getDescription());
        assertEquals(List.of("api", "quarkus"), repository.getTopics());
    }
}
