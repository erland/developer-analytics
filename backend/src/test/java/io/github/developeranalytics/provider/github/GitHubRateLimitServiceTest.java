package io.github.developeranalytics.provider.github;

import io.github.developeranalytics.provider.ProviderAccessToken;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class GitHubRateLimitServiceTest {

    private final GitHubRateLimitService service = new GitHubRateLimitService();

    @Test
    void storesLatestObservationPerCredential() {
        ProviderAccessToken token = token("first-token");
        OffsetDateTime firstAt = OffsetDateTime.of(2026, 9, 9, 8, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime laterAt = firstAt.plusMinutes(1);

        service.update(token, state(5000, 4000, firstAt.plusHours(1), firstAt));
        service.update(token, state(5000, 3900, laterAt.plusHours(1), laterAt));

        GitHubRateLimitState current = service.current(token).orElseThrow();
        assertEquals(3900, current.remaining());
        assertEquals(laterAt, current.observedAt());
    }

    @Test
    void ignoresOlderObservation() {
        ProviderAccessToken token = token("first-token");
        OffsetDateTime latestAt = OffsetDateTime.of(2026, 9, 9, 8, 10, 0, 0, ZoneOffset.UTC);

        service.update(token, state(5000, 3000, latestAt.plusHours(1), latestAt));
        GitHubRateLimitState result = service.update(
                token,
                state(5000, 4500, latestAt.plusHours(1), latestAt.minusMinutes(5))
        );

        assertEquals(3000, result.remaining());
        assertEquals(latestAt, result.observedAt());
    }

    @Test
    void keepsCredentialsIndependent() {
        ProviderAccessToken first = token("first-token");
        ProviderAccessToken second = token("second-token");
        OffsetDateTime observedAt = OffsetDateTime.now(ZoneOffset.UTC);

        service.update(first, state(5000, 0, observedAt.plusHours(1), observedAt));
        service.update(second, state(5000, 4200, observedAt.plusHours(1), observedAt));

        assertTrue(service.current(first).orElseThrow().exhausted());
        assertFalse(service.current(second).orElseThrow().exhausted());
    }

    @Test
    void equivalentTokenValueUsesSameCredentialState() {
        ProviderAccessToken firstInstance = token("shared-token");
        ProviderAccessToken secondInstance = token("shared-token");
        OffsetDateTime observedAt = OffsetDateTime.now(ZoneOffset.UTC);

        service.update(firstInstance, state(5000, 1234, observedAt.plusHours(1), observedAt));

        assertEquals(1234, service.current(secondInstance).orElseThrow().remaining());
    }

    @Test
    void clearRemovesState() {
        ProviderAccessToken token = token("first-token");
        OffsetDateTime observedAt = OffsetDateTime.now(ZoneOffset.UTC);
        service.update(token, state(5000, 2500, observedAt.plusHours(1), observedAt));

        service.clear(token);

        assertTrue(service.current(token).isEmpty());
    }

    @Test
    void normalizesBlankResourceAndDetectsExhaustion() {
        OffsetDateTime observedAt = OffsetDateTime.now(ZoneOffset.UTC);
        GitHubRateLimitState state = new GitHubRateLimitState(
                5000,
                0,
                observedAt.plusHours(1),
                observedAt,
                "  ",
                observedAt.plusMinutes(5),
                false
        );

        assertNull(state.resource());
        assertTrue(state.exhausted());
    }

    private ProviderAccessToken token(String value) {
        return new ProviderAccessToken(value);
    }

    private GitHubRateLimitState state(
            Integer limit,
            Integer remaining,
            OffsetDateTime resetAt,
            OffsetDateTime observedAt
    ) {
        return new GitHubRateLimitState(limit, remaining, resetAt, observedAt, "core", null, false);
    }
}
