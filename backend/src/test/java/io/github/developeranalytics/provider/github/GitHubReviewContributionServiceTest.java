package io.github.developeranalytics.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.provider.ProviderContribution;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("github-adapter")
class GitHubReviewContributionServiceTest {

    @Test
    void mapsUserReviewsIndependentlyOfPullRequestAuthor() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode pullRequest = mapper.readTree("""
                {
                  "number": 42,
                  "title": "PR opened by another user",
                  "updatedAt": "2026-09-09T10:00:00Z",
                  "reviews": {
                    "nodes": [
                      {
                        "databaseId": 1234,
                        "body": "Looks good",
                        "state": "APPROVED",
                        "submittedAt": "2026-09-09T09:00:00Z"
                      }
                    ]
                  }
                }
                """);

        GitHubReviewContributionService service = new GitHubReviewContributionService();
        List<ProviderContribution> result = service.mapReviewContributions(
                pullRequest, OffsetDateTime.parse("2026-09-08T00:00:00Z"));

        assertEquals(1, result.size());
        assertEquals("review-1234", result.getFirst().externalContributionId());
        assertEquals(ProviderContribution.Type.REVIEW, result.getFirst().type());
        assertEquals("Looks good", result.getFirst().title());
    }

    @Test
    void filtersOldReviewsAndMapsDismissedState() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode pullRequest = mapper.readTree("""
                {
                  "number": 7,
                  "title": "Example",
                  "updatedAt": "2026-09-09T10:00:00Z",
                  "reviews": {
                    "nodes": [
                      {
                        "databaseId": 10,
                        "body": "Old",
                        "state": "APPROVED",
                        "submittedAt": "2026-09-01T09:00:00Z"
                      },
                      {
                        "databaseId": 11,
                        "body": "",
                        "state": "DISMISSED",
                        "submittedAt": "2026-09-09T09:00:00Z"
                      }
                    ]
                  }
                }
                """);

        GitHubReviewContributionService service = new GitHubReviewContributionService();
        List<ProviderContribution> result = service.mapReviewContributions(
                pullRequest, OffsetDateTime.parse("2026-09-08T00:00:00Z"));

        assertEquals(1, result.size());
        assertEquals("review-11", result.getFirst().externalContributionId());
        assertEquals(ProviderContribution.State.CLOSED, result.getFirst().state());
        assertEquals("Review: Example", result.getFirst().title());
    }

    @Test
    void permission403DoesNotBecomeRateLimitButExhausted403Does() {
        GitHubReviewContributionService service = new GitHubReviewContributionService();

        assertNull(service.rateLimitRetryAt(
                response(403, Map.of()), "Resource not accessible by integration"));

        OffsetDateTime retryAt = service.rateLimitRetryAt(
                response(403, Map.of("X-RateLimit-Remaining", List.of("0"))),
                "API rate limit exceeded");
        assertNotNull(retryAt);
        assertTrue(retryAt.isAfter(OffsetDateTime.now().minusSeconds(1)));
    }

    private HttpResponse<String> response(int status, Map<String, List<String>> headers) {
        return new HttpResponse<>() {
            @Override public int statusCode() { return status; }
            @Override public HttpRequest request() { return HttpRequest.newBuilder(URI.create("https://api.github.com/graphql")).build(); }
            @Override public Optional<HttpResponse<String>> previousResponse() { return Optional.empty(); }
            @Override public HttpHeaders headers() { return HttpHeaders.of(headers, (a, b) -> true); }
            @Override public String body() { return ""; }
            @Override public Optional<SSLSession> sslSession() { return Optional.empty(); }
            @Override public URI uri() { return URI.create("https://api.github.com/graphql"); }
            @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
        };
    }
}
