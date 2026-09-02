package io.github.developeranalytics.provider;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record ProviderContributorStatistics(
        int contributorCount,
        int humanContributorCount,
        int botContributorCount,
        int userCommitCount,
        int repositoryCommitCount,
        long userAdditions,
        long userDeletions,
        OffsetDateTime observedAt,
        List<Week> weeks
) {
    public ProviderContributorStatistics {
        weeks = weeks == null ? List.of() : List.copyOf(weeks);
    }

    public record Week(
            LocalDate weekStart,
            int commits,
            long additions,
            long deletions
    ) {}
}
