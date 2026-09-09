package io.github.developeranalytics.provider.github;

import io.github.developeranalytics.provider.ProviderAccessToken;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Stores the latest known GitHub REST API budget per credential without retaining raw tokens. */
@ApplicationScoped
public class GitHubRateLimitService {

    @ConfigProperty(name = "developer-analytics.github.rate-limit-reserve", defaultValue = "200")
    int absoluteReserve;

    @ConfigProperty(name = "developer-analytics.github.rate-limit-reserve-percent", defaultValue = "5")
    int percentageReserve;

    private final ConcurrentMap<String, GitHubRateLimitState> states = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, OffsetDateTime> providerBlocks = new ConcurrentHashMap<>();

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

    /**
     * Applies a credential-wide temporary block without replacing the latest REST core budget.
     * This is used for provider-wide throttles such as GraphQL/secondary-rate-limit responses,
     * whose quota units must not be mixed with REST core request counts.
     */
    public void blockUntil(ProviderAccessToken accessToken, OffsetDateTime retryAt) {
        if (retryAt == null) throw new IllegalArgumentException("retryAt is required");
        String key = credentialKey(accessToken);
        providerBlocks.merge(key, retryAt,
                (current, observed) -> observed.isAfter(current) ? observed : current);
    }

    public Optional<GitHubRateLimitState> current(ProviderAccessToken accessToken) {
        if (accessToken == null) return Optional.empty();
        return Optional.ofNullable(states.get(credentialKey(accessToken)));
    }

    public GitHubRateLimitDecision decision(ProviderAccessToken accessToken) {
        return decision(accessToken, OffsetDateTime.now(ZoneOffset.UTC));
    }

    GitHubRateLimitDecision decision(ProviderAccessToken accessToken, OffsetDateTime now) {
        if (now == null) throw new IllegalArgumentException("now is required");
        String key = credentialKey(accessToken);
        OffsetDateTime providerBlock = providerBlocks.get(key);
        if (providerBlock != null) {
            if (providerBlock.isAfter(now)) {
                return GitHubRateLimitDecision.block(providerBlock, null, null, true);
            }
            providerBlocks.remove(key, providerBlock);
        }

        Optional<GitHubRateLimitState> current = Optional.ofNullable(states.get(key));
        if (current.isEmpty()) return GitHubRateLimitDecision.allow();

        GitHubRateLimitState state = current.get();
        OffsetDateTime blockedUntil = blockingUntil(state, now);
        if (blockedUntil == null) return GitHubRateLimitDecision.allow();

        int reserve = reserveFor(state.limit());
        return GitHubRateLimitDecision.block(blockedUntil, state.remaining(), reserve, state.secondaryLimited());
    }

    int reserveFor(Integer limit) {
        int absolute = Math.max(0, absoluteReserve);
        int percentage = Math.max(0, percentageReserve);
        if (limit == null || limit <= 0 || percentage == 0) return absolute;
        int percentageValue = (int) Math.ceil(limit * (percentage / 100.0d));
        return Math.max(absolute, percentageValue);
    }

    private OffsetDateTime blockingUntil(GitHubRateLimitState state, OffsetDateTime now) {
        if (state.secondaryLimited()) {
            OffsetDateTime retryAt = state.retryAt();
            if (retryAt != null && retryAt.isAfter(now)) return retryAt;
        }

        Integer remaining = state.remaining();
        if (remaining == null || remaining > reserveFor(state.limit())) return null;

        OffsetDateTime resetAt = state.resetAt();
        if (resetAt == null || !resetAt.isAfter(now)) return null;
        return resetAt;
    }

    public void clear(ProviderAccessToken accessToken) {
        if (accessToken == null) return;
        String key = credentialKey(accessToken);
        states.remove(key);
        providerBlocks.remove(key);
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

    public record GitHubRateLimitDecision(
            boolean allowed,
            OffsetDateTime resumeAt,
            Integer remaining,
            Integer reserve,
            boolean secondaryLimited
    ) {
        static GitHubRateLimitDecision allow() {
            return new GitHubRateLimitDecision(true, null, null, null, false);
        }

        static GitHubRateLimitDecision block(
                OffsetDateTime resumeAt,
                Integer remaining,
                Integer reserve,
                boolean secondaryLimited
        ) {
            return new GitHubRateLimitDecision(false, resumeAt, remaining, reserve, secondaryLimited);
        }
    }
}
