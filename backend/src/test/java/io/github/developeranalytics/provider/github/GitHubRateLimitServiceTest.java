package io.github.developeranalytics.provider.github;

import io.github.developeranalytics.provider.ProviderAccessToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class GitHubRateLimitServiceTest {

    private GitHubRateLimitService service;

    @BeforeEach
    void setUp() {
        service = new GitHubRateLimitService();
        service.absoluteReserve = 200;
        service.percentageReserve = 5;
    }

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

    @Test
    void allowsWhenNoBudgetHasBeenObserved() {
        assertTrue(service.decision(token("unknown"), OffsetDateTime.now(ZoneOffset.UTC)).allowed());
    }

    @Test
    void blocksWhenRemainingIsAtAbsoluteReserve() {
        ProviderAccessToken token = token("low-budget");
        OffsetDateTime now = OffsetDateTime.of(2026, 9, 9, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime resetAt = now.plusMinutes(20);
        service.percentageReserve = 0;
        service.update(token, state(5000, 200, resetAt, now.minusSeconds(1)));

        GitHubRateLimitService.GitHubRateLimitDecision decision = service.decision(token, now);

        assertFalse(decision.allowed());
        assertEquals(resetAt, decision.resumeAt());
        assertEquals(200, decision.reserve());
    }

    @Test
    void usesHigherPercentageReserve() {
        ProviderAccessToken token = token("percentage-budget");
        OffsetDateTime now = OffsetDateTime.of(2026, 9, 9, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime resetAt = now.plusMinutes(20);
        service.absoluteReserve = 100;
        service.percentageReserve = 10;
        service.update(token, state(5000, 450, resetAt, now.minusSeconds(1)));

        GitHubRateLimitService.GitHubRateLimitDecision decision = service.decision(token, now);

        assertFalse(decision.allowed());
        assertEquals(500, decision.reserve());
    }

    @Test
    void allowsWhenBudgetIsAboveReserve() {
        ProviderAccessToken token = token("healthy-budget");
        OffsetDateTime now = OffsetDateTime.of(2026, 9, 9, 12, 0, 0, 0, ZoneOffset.UTC);
        service.update(token, state(5000, 251, now.plusMinutes(20), now.minusSeconds(1)));

        assertTrue(service.decision(token, now).allowed());
    }

    @Test
    void passedResetNoLongerBlocksOldLowBudget() {
        ProviderAccessToken token = token("reset-budget");
        OffsetDateTime now = OffsetDateTime.of(2026, 9, 9, 12, 0, 0, 0, ZoneOffset.UTC);
        service.update(token, state(5000, 0, now.minusSeconds(1), now.minusMinutes(10)));

        assertTrue(service.decision(token, now).allowed());
    }

    @Test
    void secondaryLimitUsesRetryAtEvenWhenPrimaryBudgetIsHealthy() {
        ProviderAccessToken token = token("secondary-budget");
        OffsetDateTime now = OffsetDateTime.of(2026, 9, 9, 12, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime retryAt = now.plusMinutes(2);
        service.update(token, new GitHubRateLimitState(
                5000, 4000, now.plusHours(1), now.minusSeconds(1), "core", retryAt, true));

        GitHubRateLimitService.GitHubRateLimitDecision decision = service.decision(token, now);

        assertFalse(decision.allowed());
        assertEquals(retryAt, decision.resumeAt());
        assertTrue(decision.secondaryLimited());
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
