package io.github.developeranalytics.persistence;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class MonthValueConverterTest {

    @Test
    void convertsInstantReturnedByPostgresqlDateTrunc() {
        assertEquals(
                LocalDate.of(2025, 11, 1),
                MonthValueConverter.toMonth(Instant.parse("2025-11-01T00:00:00Z"))
        );
        assertEquals(
                LocalDate.of(2010, 8, 1),
                MonthValueConverter.toMonth(Instant.parse("2010-08-01T00:00:00Z"))
        );
    }

    @Test
    void normalizesSupportedTemporalValuesToFirstDayOfMonth() {
        LocalDate expected = LocalDate.of(2026, 9, 1);

        assertEquals(expected, MonthValueConverter.toMonth(LocalDate.of(2026, 9, 3)));
        assertEquals(expected, MonthValueConverter.toMonth(LocalDateTime.of(2026, 9, 3, 12, 30)));
        assertEquals(expected, MonthValueConverter.toMonth(OffsetDateTime.of(2026, 9, 3, 12, 30, 0, 0, ZoneOffset.UTC)));
        assertEquals(expected, MonthValueConverter.toMonth(ZonedDateTime.of(2026, 9, 3, 12, 30, 0, 0, ZoneOffset.UTC)));
        assertEquals(expected, MonthValueConverter.toMonth(java.sql.Date.valueOf("2026-09-03")));
        assertEquals(expected, MonthValueConverter.toMonth(java.sql.Timestamp.valueOf("2026-09-03 12:30:00")));
    }

    @Test
    void rejectsUnknownMonthValueTypes() {
        assertThrows(IllegalStateException.class, () -> MonthValueConverter.toMonth("2025-11-01T00:00:00Z"));
    }
}
