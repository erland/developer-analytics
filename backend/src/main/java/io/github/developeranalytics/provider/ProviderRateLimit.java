package io.github.developeranalytics.provider;

import java.time.OffsetDateTime;

public record ProviderRateLimit(
        Integer limit,
        Integer remaining,
        OffsetDateTime resetAt
) {}
