package io.github.developeranalytics.provider;

import java.time.OffsetDateTime;

public record ProviderContribution(
        String externalContributionId,
        Type type,
        String title,
        OffsetDateTime occurredAt,
        State state,
        Integer additions,
        Integer deletions,
        Integer changedFiles,
        Boolean merged
) {
    public enum Type { COMMIT, PULL_REQUEST, REVIEW, ISSUE }
    public enum State { OPEN, CLOSED, MERGED, UNKNOWN }
}
