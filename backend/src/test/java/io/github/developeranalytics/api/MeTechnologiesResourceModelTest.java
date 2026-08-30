package io.github.developeranalytics.api;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

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
                Map.of("score", 88),
                List.of(
                        new MeTechnologiesResource.TimelinePoint(
                                LocalDate.of(2026, 8, 1),
                                3,
                                12
                        )
                ),
                List.of(
                        new MeTechnologiesResource.RepresentativeProject(
                                UUID.randomUUID(),
                                "demo",
                                "https://github.com/example/demo",
                                "PUBLIC",
                                "OWNED_BY_USER",
                                OffsetDateTime.parse("2026-08-20T08:00:00Z"),
                                3
                        )
                )
        );

        assertEquals("STRONG", entry.evidenceLevel());
        assertEquals(6, entry.projectCount());
        assertEquals(12, entry.timeline().getFirst().activityCount());
        assertEquals("demo", entry.representativeProjects().getFirst().repositoryName());
    }
}
