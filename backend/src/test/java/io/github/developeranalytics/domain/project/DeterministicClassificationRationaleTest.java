package io.github.developeranalytics.domain.project;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import org.junit.jupiter.api.Test;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class DeterministicClassificationRationaleTest {
    @Test
    void deterministicAssignmentKeepsSignalsExplainable() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(
                user, "github", "repo-1", "alice", "api-service");

        ProjectCategory api = new ProjectCategory(
                "api", "API", null, List.of(), 10);

        RepositoryProjectCategory classification =
                new RepositoryProjectCategory(
                        repository,
                        api,
                        RepositoryProjectCategory.Source.DETERMINISTIC,
                        RepositoryProjectCategory.Confidence.HIGH,
                        Map.of(
                                "score", 9,
                                "signals", List.of(
                                        "topic:api",
                                        "technology:quarkus")),
                        OffsetDateTime.parse("2026-08-30T08:00:00Z")
                );

        assertEquals(
                RepositoryProjectCategory.Source.DETERMINISTIC,
                classification.getSource());
        assertTrue(
                ((List<?>) classification.getRationale().get("signals"))
                        .contains("topic:api"));
    }
}
