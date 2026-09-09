package io.github.developeranalytics.provider.github;

import java.time.OffsetDateTime;

/** Latest observed GitHub REST API budget for one authenticated user/provider connection. */
public record GitHubRateLimitState(
        Integer limit,
        Integer remaining,
        OffsetDateTime resetAt,
        OffsetDateTime observedAt,
        String resource,
        OffsetDateTime retryAt,
        boolean secondaryLimited
) {
    public GitHubRateLimitState {
        if (observedAt == null) throw new IllegalArgumentException("observedAt is required");
        resource = resource == null || resource.isBlank() ? null : resource.strip();
    }

    public boolean exhausted() {
        return remaining != null && remaining <= 0;
    }
}
