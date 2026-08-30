package io.github.developeranalytics.provider;

public record ProviderUser(
        String externalUserId,
        String login,
        String displayName
) {}
