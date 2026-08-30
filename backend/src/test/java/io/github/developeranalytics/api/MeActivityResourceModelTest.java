package io.github.developeranalytics.api;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MeActivityResourceModelTest {

    @Test
    void activityResponseKeepsMonthlyAndYearlyViewsSeparate() {
        var response = new MeActivityResource.ActivityResponse(
                42,
                6,
                18.5,
                11.0,
                500,
                210,
                OffsetDateTime.parse("2026-01-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-20T00:00:00Z"),
                List.of(new MeActivityResource.YearPoint(2026, 42)),
                List.of(new MeActivityResource.MonthPoint("2026-08", 12, 3))
        );

        assertEquals(42, response.commitCount());
        assertEquals(18.5, response.averageCommitSize());
        assertEquals(11.0, response.medianCommitSize());
        assertEquals(3, response.commitsPerMonth().getFirst().activeProjects());
    }
}
