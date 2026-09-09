package io.github.developeranalytics.provider.github;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Stores the latest known GitHub REST API budget per authenticated application user. */
@ApplicationScoped
public class GitHubRateLimitService {

    private final ConcurrentMap<UUID, GitHubRateLimitState> states = new ConcurrentHashMap<>();

    public GitHubRateLimitState update(UUID userId, GitHubRateLimitState observed) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (observed == null) throw new IllegalArgumentException("observed rate limit state is required");

        return states.compute(userId, (ignored, current) -> {
            if (current == null || !observed.observedAt().isBefore(current.observedAt())) {
                return observed;
            }
            return current;
        });
    }

    public Optional<GitHubRateLimitState> current(UUID userId) {
        if (userId == null) return Optional.empty();
        return Optional.ofNullable(states.get(userId));
    }

    public void clear(UUID userId) {
        if (userId != null) states.remove(userId);
    }
}
