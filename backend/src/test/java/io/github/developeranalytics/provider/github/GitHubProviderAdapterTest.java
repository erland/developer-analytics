package io.github.developeranalytics.provider.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("github-adapter")
class GitHubProviderAdapterTest {

    private GitHubProviderAdapter adapter;
    private GitHubRateLimitService rateLimits;

    @BeforeEach
    void setUp() {
        adapter = new GitHubProviderAdapter();
        adapter.mapper = new ObjectMapper();
        rateLimits = new GitHubRateLimitService();
        adapter.rateLimits = rateLimits;
    }

    @Test
    void mapsRepositoryIntoProviderNeutralModel() throws Exception {
        String body = "{" +
                "\"id\":42," +
                "\"name\":\"developer-analytics\"," +
                "\"full_name\":\"erland/developer-analytics\"," +
                "\"html_url\":\"https://github.com/erland/developer-analytics\"," +
                "\"private\":true," +
                "\"fork\":false," +
                "\"archived\":false," +
                "\"size\":1234," +
                "\"created_at\":\"2026-08-01T10:00:00Z\"," +
                "\"updated_at\":\"2026-08-30T07:00:00Z\"," +
                "\"pushed_at\":\"2026-08-30T06:59:00Z\"," +
                "\"owner\":{\"id\":99,\"login\":\"erland\",\"type\":\"User\"}" +
                "}";

        ProviderRepository repository = adapter.mapRepository(adapter.mapper.readTree(body));

        assertEquals("42", repository.externalRepositoryId());
        assertEquals("99", repository.ownerExternalId());
        assertEquals("erland", repository.ownerLogin());
        assertEquals(ProviderRepository.OwnerType.USER, repository.ownerType());
        assertEquals(ProviderRepository.Visibility.PRIVATE, repository.visibility());
        assertEquals("developer-analytics", repository.name());
        assertEquals(1234L * 1024L, repository.repositorySizeBytes());
        assertFalse(repository.fork());
        assertFalse(repository.archived());
    }

    @Test
    void observesSuccessfulRateLimitHeaders() {
        ProviderAccessToken token = new ProviderAccessToken("success-token");
        OffsetDateTime resetAt = OffsetDateTime.now(ZoneOffset.UTC)
                .plusHours(1)
                .truncatedTo(ChronoUnit.SECONDS);

        adapter.observeRateLimit(token, response(200, Map.of(
                "X-RateLimit-Limit", List.of("5000"),
                "X-RateLimit-Remaining", List.of("4321"),
                "X-RateLimit-Reset", List.of(Long.toString(resetAt.toEpochSecond())),
                "X-RateLimit-Resource", List.of("core")
        )), null);

        GitHubRateLimitState state = rateLimits.current(token).orElseThrow();
        assertEquals(5000, state.limit());
        assertEquals(4321, state.remaining());
        assertEquals(resetAt, state.resetAt());
        assertEquals("core", state.resource());
        assertNull(state.retryAt());
        assertFalse(state.secondaryLimited());
    }

    @Test
    void observesExhaustedSecondaryRateLimit() {
        ProviderAccessToken token = new ProviderAccessToken("secondary-token");
        OffsetDateTime resetAt = OffsetDateTime.now(ZoneOffset.UTC)
                .plusHours(1)
                .truncatedTo(ChronoUnit.SECONDS);

        adapter.observeRateLimit(token, response(403, Map.of(
                "X-RateLimit-Limit", List.of("5000"),
                "X-RateLimit-Remaining", List.of("0"),
                "X-RateLimit-Reset", List.of(Long.toString(resetAt.toEpochSecond())),
                "X-RateLimit-Resource", List.of("core")
        )), "You have exceeded a secondary rate limit.");

        GitHubRateLimitState state = rateLimits.current(token).orElseThrow();
        assertTrue(state.exhausted());
        assertTrue(state.secondaryLimited());
        assertNotNull(state.retryAt());
        assertFalse(state.retryAt().isBefore(resetAt));
    }

    @Test
    void permissionFailureWithoutRateLimitHeadersDoesNotCreateBudgetState() {
        ProviderAccessToken token = new ProviderAccessToken("permission-token");

        adapter.observeRateLimit(token, response(403, Map.of()), "Resource not accessible by integration");

        assertTrue(rateLimits.current(token).isEmpty());
    }

    @Test
    void accessTokenDoesNotLeakThroughToString() {
        assertEquals("[REDACTED]", new ProviderAccessToken("secret").toString());
    }

    private HttpResponse<String> response(int status, Map<String, List<String>> headers) {
        return new StubResponse(status, HttpHeaders.of(headers, (name, value) -> true));
    }

    private record StubResponse(int statusCode, HttpHeaders headers) implements HttpResponse<String> {
        @Override public HttpRequest request() { return null; }
        @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
        @Override public String body() { return ""; }
        @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return URI.create("https://api.github.com/user"); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
