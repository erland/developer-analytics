package io.github.developeranalytics.domain.project;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RepositoryProjectCategoryTest {

    @Test
    void repositoryCanHaveMultipleCategories() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(
                user, "github", "repo-1", "alice", "demo");

        ProjectCategory web = new ProjectCategory(
                "web-application", "Web application", null, null, 10);
        ProjectCategory api = new ProjectCategory(
                "api", "API", null, null, 20);

        RepositoryProjectCategory first =
                new RepositoryProjectCategory(
                        repository,
                        web,
                        RepositoryProjectCategory.Source.DETERMINISTIC,
                        RepositoryProjectCategory.Confidence.HIGH,
                        Map.of("signal", "package.json"),
                        OffsetDateTime.parse("2026-08-30T08:00:00Z")
                );

        RepositoryProjectCategory second =
                new RepositoryProjectCategory(
                        repository,
                        api,
                        RepositoryProjectCategory.Source.DETERMINISTIC,
                        RepositoryProjectCategory.Confidence.MEDIUM,
                        Map.of("signal", "backend"),
                        OffsetDateTime.parse("2026-08-30T08:00:00Z")
                );

        assertEquals("web-application", first.getCategory().getCategoryKey());
        assertEquals("api", second.getCategory().getCategoryKey());
    }
}
