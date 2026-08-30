package io.github.developeranalytics.domain.project;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ProjectCategoryTest {

    @Test
    void normalizesStableCategoryKey() {
        ProjectCategory category = new ProjectCategory(
                "Web Application",
                "Web application",
                null,
                List.of("web app"),
                10
        );

        assertEquals("web-application", category.getCategoryKey());
        assertEquals(List.of("web app"), category.getAliases());
    }

    @Test
    void taxonomyIsNotAnEnum() {
        assertFalse(ProjectCategory.class.isEnum());
    }
}
