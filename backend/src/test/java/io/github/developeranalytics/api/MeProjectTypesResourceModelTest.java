package io.github.developeranalytics.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MeProjectTypesResourceModelTest {
    @Test
    void categoryViewContainsCountsTimelineAndRepresentativeProjects() {
        var entry = new MeProjectTypesResource.Entry(
                "backend-service", "Backend service", 4, 120,
                List.of(new MeProjectTypesResource.TimelinePoint("2026-08", 30, 900, 28, 3)),
                List.of(new MeProjectTypesResource.RepresentativeProject(
                        UUID.randomUUID(), "demo-service", "https://github.com/example/demo-service",
                        "PUBLIC", "OWNED_BY_USER", OffsetDateTime.parse("2026-08-20T08:00:00Z"), 42))
        );

        assertEquals(4, entry.projectCount());
        assertEquals(120, entry.activityCount());
        assertEquals(30, entry.timeline().getFirst().commits());
        assertEquals(900, entry.timeline().getFirst().changedLines());
        assertEquals(3, entry.timeline().getFirst().activeProjectCount());
        assertEquals("demo-service", entry.representativeProjects().getFirst().repositoryName());
    }
}
