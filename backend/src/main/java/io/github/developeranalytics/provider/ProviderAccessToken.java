package io.github.developeranalytics.provider;

import java.util.Objects;

public record ProviderAccessToken(String value) {
    public ProviderAccessToken {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) throw new IllegalArgumentException("Provider access token must not be blank");
    }

    @Override
    public String toString() {
        return "[REDACTED]";
    }
}
