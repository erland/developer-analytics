package io.github.developeranalytics.api;

import jakarta.ws.rs.BadRequestException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

/**
 * Normalises the time-related AnalysisScope query parameters into an inclusive
 * date range. Multiple supplied constraints are intersected, so hierarchical
 * selections such as year + month + week remain valid and predictable.
 */
final class AnalysisPeriod {

    private AnalysisPeriod() {
    }

    static Range resolve(String from, String to, Integer year, String month, String week) {
        LocalDate start = parseDate(from, "from");
        LocalDate end = parseDate(to, "to");

        if (year != null) {
            if (year < 1970 || year > 9999) {
                throw new BadRequestException("year must be between 1970 and 9999");
            }
            start = later(start, LocalDate.of(year, 1, 1));
            end = earlier(end, LocalDate.of(year, 12, 31));
        }

        if (month != null && !month.isBlank()) {
            YearMonth parsedMonth;
            try {
                parsedMonth = YearMonth.parse(month.trim());
            } catch (DateTimeParseException error) {
                throw new BadRequestException("month must use YYYY-MM format");
            }
            start = later(start, parsedMonth.atDay(1));
            end = earlier(end, parsedMonth.atEndOfMonth());
        }

        if (week != null && !week.isBlank()) {
            Range weekRange = parseWeek(week.trim());
            start = later(start, weekRange.from());
            end = earlier(end, weekRange.to());
        }

        if (start != null && end != null && start.isAfter(end)) {
            throw new BadRequestException("time filters do not overlap");
        }

        return new Range(start, end);
    }

    private static Range parseWeek(String value) {
        // The Timeline API currently exposes concrete week-start dates. Accept
        // ISO week notation as well so URL state can remain readable/stable.
        if (value.matches("\\d{4}-W\\d{2}")) {
            try {
                int year = Integer.parseInt(value.substring(0, 4));
                int week = Integer.parseInt(value.substring(6, 8));
                LocalDate monday = LocalDate.of(year, 1, 4)
                        .with(IsoFields.WEEK_OF_WEEK_BASED_YEAR, week)
                        .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                return new Range(monday, monday.plusDays(6));
            } catch (RuntimeException error) {
                throw new BadRequestException("week must use YYYY-MM-DD or YYYY-Www format");
            }
        }

        LocalDate start = parseDate(value, "week");
        return new Range(start, start.plusDays(6));
    }

    private static LocalDate parseDate(String value, String parameter) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException error) {
            throw new BadRequestException(parameter + " must use YYYY-MM-DD format");
        }
    }

    private static LocalDate later(LocalDate current, LocalDate candidate) {
        return current == null || candidate.isAfter(current) ? candidate : current;
    }

    private static LocalDate earlier(LocalDate current, LocalDate candidate) {
        return current == null || candidate.isBefore(current) ? candidate : current;
    }

    record Range(LocalDate from, LocalDate to) {
        boolean constrained() {
            return from != null || to != null;
        }
    }
}
