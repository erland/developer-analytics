package io.github.developeranalytics.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MeActivityResourceModelTest {

    @Test
    void activityResponseKeepsPeriodViewsAndLineMetricsSeparate() {
        var response = new MeActivityResource.ActivityResponse(
                42,
                6,
                18.5,
                11.0,
                500,
                210,
                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-20T00:00:00Z"),
                List.of(new MeActivityResource.YearPoint(2026, 42, 500, 210, 710, 40, 3, List.of("demo"))),
                List.of(new MeActivityResource.MonthPoint("2026-08", 12, 140, 60, 200, 11, 3, List.of("demo"))),
                List.of(new MeActivityResource.WeekPoint("2026-08-17", 4, 50, 20, 70, 4, 2, List.of("demo"))),
                List.of(),
                true,
                40
        );

        assertEquals(42, response.commitCount());
        assertEquals(18.5, response.averageCommitSize());
        assertEquals(11.0, response.medianCommitSize());
        assertEquals(3, response.commitsPerMonth().getFirst().activeProjects());
        assertEquals(710, response.commitsPerYear().getFirst().changedLines());
        assertEquals(40, response.lineStatisticsCommitCount());
    }
    @Test
    void projectLifecycleKeepsAllTechnologiesAndProjectTypesAlongsidePrimaryLabels() {
        var lifecycle = new MeActivityResource.ProjectLifecycle(
                java.util.UUID.randomUUID(),
                "demo",
                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-20T00:00:00Z"),
                12,
                "Backend service",
                "Java",
                List.of("Backend service", "CLI"),
                List.of("Java", "Quarkus", "PostgreSQL"),
                List.of(),
                List.of()
        );

        assertEquals("Java", lifecycle.technology());
        assertEquals(List.of("Java", "Quarkus", "PostgreSQL"), lifecycle.technologies());
        assertEquals(List.of("Backend service", "CLI"), lifecycle.projectTypes());
    }

}
