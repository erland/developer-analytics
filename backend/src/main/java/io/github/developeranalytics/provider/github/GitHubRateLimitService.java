package io.github.developeranalytics.provider.github;

import io.github.developeranalytics.provider.ProviderAccessToken;
import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Stores the latest known GitHub REST API budget per credential without retaining raw tokens. */
@ApplicationScoped
public class GitHubRateLimitService {

    private final ConcurrentMap<String, GitHubRateLimitState> states = new ConcurrentHashMap<>();

    public GitHubRateLimitState update(ProviderAccessToken accessToken, GitHubRateLimitState observed) {
        String key = credentialKey(accessToken);
        if (observed == null) throw new IllegalArgumentException("observed rate limit state is required");

        return states.compute(key, (ignored, current) -> {
            if (current == null || !observed.observedAt().isBefore(current.observedAt())) {
                return observed;
            }
            return current;
        });
    }

    public Optional<GitHubRateLimitState> current(ProviderAccessToken accessToken) {
        if (accessToken == null) return Optional.empty();
        return Optional.ofNullable(states.get(credentialKey(accessToken)));
    }

    public void clear(ProviderAccessToken accessToken) {
        if (accessToken != null) states.remove(credentialKey(accessToken));
    }

    private String credentialKey(ProviderAccessToken accessToken) {
        if (accessToken == null || accessToken.value() == null || accessToken.value().isBlank()) {
            throw new IllegalArgumentException("accessToken is required");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(accessToken.value().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
