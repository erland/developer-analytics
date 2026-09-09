package io.github.developeranalytics.service.connection;

import io.github.developeranalytics.provider.ProviderAccessToken;

import java.util.Objects;

/** Provider credentials and persisted identity resolved from one connection lookup. */
public record ProviderSession(
        ProviderAccessToken accessToken,
        String login
) {
    public ProviderSession {
        Objects.requireNonNull(accessToken, "accessToken");
        login = login == null || login.isBlank() ? null : login;
    }
}
