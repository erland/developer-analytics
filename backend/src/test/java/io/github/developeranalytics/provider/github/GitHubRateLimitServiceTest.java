package io.github.developeranalytics.provider.github;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class GitHubRateLimitServiceTest {

    private final GitHubRateLimitService service = new GitHubRateLimitService();

    @Test
    void storesLatestObservationPerUser() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime firstAt = OffsetDateTime.of(2026, 9, 9, 8, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime laterAt = firstAt.plusMinutes(1);

        service.update(userId, state(5000, 4000, firstAt.plusHours(1), firstAt));
        service.update(userId, state(5000, 3900, laterAt.plusHours(1), laterAt));

        GitHubRateLimitState current = service.current(userId).orElseThrow();
        assertEquals(3900, current.remaining());
        assertEquals(laterAt, current.observedAt());
    }

    @Test
    void ignoresOlderObservation() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime latestAt = OffsetDateTime.of(2026, 9, 9, 8, 10, 0, 0, ZoneOffset.UTC);

        service.update(userId, state(5000, 3000, latestAt.plusHours(1), latestAt));
        GitHubRateLimitState result = service.update(
                userId,
                state(5000, 4500, latestAt.plusHours(1), latestAt.minusMinutes(5))
        );

        assertEquals(3000, result.remaining());
        assertEquals(latestAt, result.observedAt());
    }

    @Test
    void keepsUsersIndependent() {
        UUID firstUser = UUID.randomUUID();
        UUID secondUser = UUID.randomUUID();
        OffsetDateTime observedAt = OffsetDateTime.now(ZoneOffset.UTC);

        service.update(firstUser, state(5000, 0, observedAt.plusHours(1), observedAt));
        service.update(secondUser, state(5000, 4200, observedAt.plusHours(1), observedAt));

        assertTrue(service.current(firstUser).orElseThrow().exhausted());
        assertFalse(service.current(secondUser).orElseThrow().exhausted());
    }

    @Test
    void clearRemovesState() {
        UUID userId = UUID.randomUUID();
        OffsetDateTime observedAt = OffsetDateTime.now(ZoneOffset.UTC);
        service.update(userId, state(5000, 2500, observedAt.plusHours(1), observedAt));

        service.clear(userId);

        assertTrue(service.current(userId).isEmpty());
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

    private GitHubRateLimitState state(
            Integer limit,
            Integer remaining,
            OffsetDateTime resetAt,
            OffsetDateTime observedAt
    ) {
        return new GitHubRateLimitState(limit, remaining, resetAt, observedAt, "core", null, false);
    }
}
