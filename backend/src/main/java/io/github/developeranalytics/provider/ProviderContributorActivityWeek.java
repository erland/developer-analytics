package io.github.developeranalytics.provider;

import java.time.LocalDate;

public record ProviderContributorActivityWeek(
        LocalDate weekStart,
        int commits,
        long additions,
        long deletions
) {}
