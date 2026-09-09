package io.github.developeranalytics.provider.history;

import io.github.developeranalytics.provider.ProviderContributionFileChange;

import java.util.List;
import java.util.Objects;

/**
 * Provider-neutral changed-file statistics for one historical commit.
 */
public record HistoricalCommitFileChanges(
        String commitSha,
        List<ProviderContributionFileChange> fileChanges
) {
    public HistoricalCommitFileChanges {
        if (commitSha == null || commitSha.isBlank()) {
            throw new IllegalArgumentException("commitSha must not be blank");
        }
        Objects.requireNonNull(fileChanges, "fileChanges");
        fileChanges = List.copyOf(fileChanges);
    }
}
