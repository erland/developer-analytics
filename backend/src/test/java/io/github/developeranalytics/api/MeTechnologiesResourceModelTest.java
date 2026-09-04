package io.github.developeranalytics.api;

import io.github.developeranalytics.persistence.technology.TechnologyTimelineRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MeTechnologiesResourceModelTest {

    @Test
    void technologyViewContainsTimelineAndRepresentativeProjects() {
        var entry = new MeTechnologiesResource.Entry(
                "java",
                "Java",
                "LANGUAGE",
                "STRONG",
                88,
                6,
                14,
                3,
                OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-20T08:00:00Z"),
                4,
                "INCLUDES_PRIVATE",
                Map.of("score", 88),
                List.of(new MeTechnologiesResource.TimelinePoint("2026-08", 12, 360, 11, 3)),
                List.of(new MeTechnologiesResource.RepresentativeProject(
                        UUID.randomUUID(),
                        "demo",
                        "https://github.com/example/demo",
                        "PUBLIC",
                        "OWNED_BY_USER",
                        OffsetDateTime.parse("2026-08-20T08:00:00Z"),
                        3
                ))
        );

        assertEquals("STRONG", entry.evidenceLevel());
        assertEquals(6, entry.projectCount());
        assertEquals(12, entry.timeline().getFirst().commits());
        assertEquals(360, entry.timeline().getFirst().changedLines());
        assertEquals(3, entry.timeline().getFirst().projectCount());
        assertEquals("demo", entry.representativeProjects().getFirst().repositoryName());
    }
    @Test
    void technologyTimelineOnlyKeepsPeriodsWithActualActivity() {
        assertFalse(MeTechnologiesResource.hasActivity(
                new TechnologyTimelineRepository.MetricActivityRow("java", "2025-01", 0, 0, 0, 0)));
        assertTrue(MeTechnologiesResource.hasActivity(
                new TechnologyTimelineRepository.MetricActivityRow("java", "2025-02", 1, 0, 0, 1)));
        assertTrue(MeTechnologiesResource.hasActivity(
                new TechnologyTimelineRepository.MetricActivityRow("java", "2025-03", 0, 10, 1, 0)));
        assertTrue(MeTechnologiesResource.hasActivity(
                new TechnologyTimelineRepository.MetricActivityRow("java", "2025-04", 0, 0, 1, 0)));
    }

}
