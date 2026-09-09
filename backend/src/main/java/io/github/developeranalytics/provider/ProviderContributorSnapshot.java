package io.github.developeranalytics.provider;

import java.util.Collections;
import java.util.List;

public record ProviderContributorSnapshot(
        ProviderContributorStatistics statistics,
        List<ProviderContributorActivityWeek> userActivityWeeks
) {
    public ProviderContributorSnapshot {
        userActivityWeeks = userActivityWeeks == null
                ? List.of()
                : Collections.unmodifiableList(userActivityWeeks);
    }
}
