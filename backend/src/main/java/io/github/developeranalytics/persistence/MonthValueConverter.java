package io.github.developeranalytics.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

public final class MonthValueConverter {
    private MonthValueConverter() {
    }

    public static LocalDate toMonth(Object value) {
        if (value instanceof java.sql.Timestamp v) return v.toLocalDateTime().toLocalDate().withDayOfMonth(1);
        if (value instanceof LocalDateTime v) return v.toLocalDate().withDayOfMonth(1);
        if (value instanceof OffsetDateTime v) return v.toLocalDate().withDayOfMonth(1);
        if (value instanceof ZonedDateTime v) return v.toLocalDate().withDayOfMonth(1);
        if (value instanceof Instant v) return v.atZone(ZoneOffset.UTC).toLocalDate().withDayOfMonth(1);
        if (value instanceof LocalDate v) return v.withDayOfMonth(1);
        if (value instanceof java.sql.Date v) return v.toLocalDate().withDayOfMonth(1);
        throw new IllegalStateException("Unsupported month value: " + value);
    }
}
