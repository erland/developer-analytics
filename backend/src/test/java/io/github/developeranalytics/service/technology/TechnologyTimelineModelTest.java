package io.github.developeranalytics.service.technology;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class TechnologyTimelineModelTest {

    @Test
    void timelineIsChartReadyAndKeepsVisibilitySeparate() {
        var timeline =
                new TechnologyTimelineService.TechnologyTimeline(
                        "java",
                        OffsetDateTime.parse("2024-01-01T00:00:00Z"),
                        OffsetDateTime.parse("2026-08-30T08:00:00Z"),
                        6,
                        4,
                        2,
                        List.of(
                                new TechnologyTimelineService.YearPoint(
                                        2025,
                                        3,
                                        120
                                ),
                                new TechnologyTimelineService.YearPoint(
                                        2026,
                                        4,
                                        180
                                )
                        )
                );

        assertEquals("java", timeline.technologyKey());
        assertEquals(4, timeline.publicRepositoryCount());
        assertEquals(2, timeline.privateRepositoryCount());
        assertEquals(2, timeline.years().size());
        assertEquals(180, timeline.years().get(1).activityCount());
    }
}
