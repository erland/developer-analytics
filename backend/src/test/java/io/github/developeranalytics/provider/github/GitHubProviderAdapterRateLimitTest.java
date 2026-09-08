package io.github.developeranalytics.provider.github;

import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.*;

@Tag("github-adapter")
class GitHubProviderAdapterRateLimitTest {

    @Test
    void providerExceptionCarriesRetryAtForPrimaryRateLimit() throws Exception {
        GitHubProviderAdapter adapter = new GitHubProviderAdapter();
        java.lang.reflect.Method method = GitHubProviderAdapter.class.getDeclaredMethod(
                "rateLimitRetryAt", HttpResponse.class, String.class);
        method.setAccessible(true);

        long resetEpoch = java.time.Instant.now().plusSeconds(120).getEpochSecond();
        OffsetDateTime retryAt = (OffsetDateTime) method.invoke(adapter,
                response(403, Map.of(
                        "X-RateLimit-Remaining", List.of("0"),
                        "X-RateLimit-Reset", List.of(Long.toString(resetEpoch)))),
                "API rate limit exceeded");

        assertNotNull(retryAt);
        assertTrue(retryAt.toEpochSecond() >= resetEpoch);
    }

    @Test
    void nonRateLimit403DoesNotGetRetryAt() throws Exception {
        GitHubProviderAdapter adapter = new GitHubProviderAdapter();
        java.lang.reflect.Method method = GitHubProviderAdapter.class.getDeclaredMethod(
                "rateLimitRetryAt", HttpResponse.class, String.class);
        method.setAccessible(true);

        OffsetDateTime retryAt = (OffsetDateTime) method.invoke(adapter,
                response(403, Map.of()),
                "Resource not accessible by integration");

        assertNull(retryAt);
    }

    private static HttpResponse<String> response(int status, Map<String, List<String>> headers) {
        return new HttpResponse<>() {
            @Override public int statusCode() { return status; }
            @Override public HttpRequest request() { return HttpRequest.newBuilder(URI.create("https://api.github.com/test")).build(); }
            @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
            @Override public HttpHeaders headers() { return HttpHeaders.of(headers, (a, b) -> true); }
            @Override public String body() { return ""; }
            @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
            @Override public URI uri() { return URI.create("https://api.github.com/test"); }
            @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        };
    }
}
