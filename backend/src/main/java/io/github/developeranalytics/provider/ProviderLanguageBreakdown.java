package io.github.developeranalytics.provider;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ProviderLanguageBreakdown(
        Map<String, Long> bytesByLanguage,
        ProviderRateLimit rateLimit
) {
    public ProviderLanguageBreakdown {
        bytesByLanguage = bytesByLanguage == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(bytesByLanguage));
    }
}
