package io.github.developeranalytics.api;

import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class AnalysisPeriodTest {

    @Test
    void leavesPeriodUnconstrainedWhenNoTimeFiltersAreProvided() {
        var range = AnalysisPeriod.resolve(null, null, null, null, null);

        assertNull(range.from());
        assertNull(range.to());
    }

    @Test
    void resolvesYearAndMonthByIntersection() {
        var range = AnalysisPeriod.resolve(null, null, 2026, "2026-08", null);

        assertEquals(LocalDate.of(2026, 8, 1), range.from());
        assertEquals(LocalDate.of(2026, 8, 31), range.to());
    }

    @Test
    void resolvesConcreteWeekStartAsSevenDayRange() {
        var range = AnalysisPeriod.resolve(null, null, null, null, "2026-08-03");

        assertEquals(LocalDate.of(2026, 8, 3), range.from());
        assertEquals(LocalDate.of(2026, 8, 9), range.to());
    }

    @Test
    void resolvesIsoWeekNotation() {
        var range = AnalysisPeriod.resolve(null, null, null, null, "2026-W32");

        assertEquals(LocalDate.of(2026, 8, 3), range.from());
        assertEquals(LocalDate.of(2026, 8, 9), range.to());
    }

    @Test
    void intersectsExplicitBoundsWithHigherLevelPeriod() {
        var range = AnalysisPeriod.resolve("2026-08-10", "2026-09-10", 2026, "2026-08", null);

        assertEquals(LocalDate.of(2026, 8, 10), range.from());
        assertEquals(LocalDate.of(2026, 8, 31), range.to());
    }

    @Test
    void rejectsNonOverlappingTimeFilters() {
        assertThrows(BadRequestException.class,
                () -> AnalysisPeriod.resolve("2025-01-01", "2025-12-31", 2026, null, null));
    }

    @Test
    void rejectsInvalidMonthAndWeekFormats() {
        assertThrows(BadRequestException.class,
                () -> AnalysisPeriod.resolve(null, null, null, "August 2026", null));
        assertThrows(BadRequestException.class,
                () -> AnalysisPeriod.resolve(null, null, null, null, "week-32"));
    }
}
