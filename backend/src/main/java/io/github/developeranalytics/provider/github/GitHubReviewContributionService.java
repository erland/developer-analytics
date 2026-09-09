package io.github.developeranalytics.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderContribution;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Fetches a user's pull request review contributions without one REST request per pull request. */
@ApplicationScoped
public class GitHubReviewContributionService {
    private static final URI GRAPHQL_URI = URI.create("https://api.github.com/graphql");
    private static final int PULL_REQUEST_PAGE_SIZE = 50;

    @Inject ObjectMapper mapper;
    @Inject GitHubApiUsageTracker usage;
    @Inject GitHubRateLimitService rateLimits;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public List<ProviderContribution> fetch(
            ProviderAccessToken accessToken,
            ProviderRepository repository,
            OffsetDateTime since,
            String userLogin
    ) throws ProviderException {
        if (accessToken == null || accessToken.value() == null || accessToken.value().isBlank()) {
            throw new ProviderException("GitHub access token is required", 0);
        }
        if (userLogin == null || userLogin.isBlank()) return List.of();
        String fullName = repository == null ? null : repository.fullName();
        if (fullName == null || !fullName.contains("/")) {
            throw new ProviderException("GitHub repository full name is required", 0);
        }

        String[] parts = fullName.split("/", 2);
        String cursor = null;
        List<ProviderContribution> result = new ArrayList<>();
        boolean complete = false;

        while (!complete) {
            JsonNode data = execute(accessToken, parts[0], parts[1], userLogin, cursor);
            JsonNode pullRequests = data.path("repository").path("pullRequests");
            if (!pullRequests.isObject()) {
                throw new ProviderException("GitHub GraphQL review response did not contain pull requests", 0);
            }

            boolean pageOlderThanSince = since != null;
            for (JsonNode pullRequest : pullRequests.path("nodes")) {
                OffsetDateTime updatedAt = parseDate(pullRequest, "updatedAt");
                if (since == null || updatedAt == null || !updatedAt.isBefore(since)) {
                    pageOlderThanSince = false;
                }
                result.addAll(mapReviewContributions(pullRequest, since));
            }

            JsonNode pageInfo = pullRequests.path("pageInfo");
            boolean hasNextPage = pageInfo.path("hasNextPage").asBoolean(false);
            cursor = pageInfo.path("endCursor").isTextual() ? pageInfo.path("endCursor").asText() : null;
            complete = !hasNextPage || cursor == null || pageOlderThanSince;
        }

        return result;
    }

    JsonNode execute(
            ProviderAccessToken accessToken,
            String owner,
            String name,
            String userLogin,
            String cursor
    ) throws ProviderException {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("owner", owner);
        variables.put("name", name);
        variables.put("login", userLogin);
        variables.put("cursor", cursor);

        String body;
        try {
            body = mapper.writeValueAsString(Map.of("query", query(), "variables", variables));
        } catch (Exception e) {
            throw new ProviderException("Could not build GitHub GraphQL review request", 0, e);
        }

        HttpRequest request = HttpRequest.newBuilder(GRAPHQL_URI)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + accessToken.value())
                .header("X-GitHub-Api-Version", GitHubProviderAdapter.API_VERSION)
                .header("User-Agent", "developer-analytics")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        usage.record("reviews-graphql");

        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            String providerMessage = graphqlErrorMessage(responseBody);
            OffsetDateTime retryAt = rateLimitRetryAt(response, providerMessage);
            if (retryAt != null) rateLimits.blockUntil(accessToken, retryAt);

            if (response.statusCode() / 100 != 2) {
                throw new ProviderException(
                        "GitHub GraphQL reviews request failed with HTTP " + response.statusCode(),
                        response.statusCode(), retryAt);
            }
            JsonNode json = mapper.readTree(responseBody);
            JsonNode errors = json.path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                String message = errors.get(0).path("message").asText("GitHub GraphQL review query failed");
                OffsetDateTime graphqlRetryAt = signalsRateLimit(message)
                        ? OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(1)
                        : null;
                if (graphqlRetryAt != null) rateLimits.blockUntil(accessToken, graphqlRetryAt);
                throw new ProviderException("GitHub GraphQL reviews failed: " + message, 0, graphqlRetryAt);
            }
            return json.path("data");
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderException("GitHub GraphQL reviews request failed", 0, e);
        }
    }

    List<ProviderContribution> mapReviewContributions(JsonNode pullRequest, OffsetDateTime since) {
        List<ProviderContribution> result = new ArrayList<>();
        String pullTitle = pullRequest.path("title").asText("Pull request review");

        for (JsonNode review : pullRequest.path("reviews").path("nodes")) {
            OffsetDateTime submittedAt = parseDate(review, "submittedAt");
            if (submittedAt == null || (since != null && submittedAt.isBefore(since))) continue;

            String id = review.path("databaseId").asText("");
            if (id.isBlank()) continue;
            String reviewBody = review.path("body").asText("");
            String title = reviewBody.isBlank() ? "Review: " + pullTitle : reviewBody;
            ProviderContribution.State state = "DISMISSED".equalsIgnoreCase(review.path("state").asText(""))
                    ? ProviderContribution.State.CLOSED
                    : ProviderContribution.State.UNKNOWN;
            result.add(new ProviderContribution(
                    "review-" + id,
                    ProviderContribution.Type.REVIEW,
                    title,
                    submittedAt,
                    state,
                    null,
                    null,
                    null,
                    null
            ));
        }
        return result;
    }

    OffsetDateTime rateLimitRetryAt(HttpResponse<?> response, String providerMessage) {
        int status = response.statusCode();
        String remaining = response.headers().firstValue("X-RateLimit-Remaining").orElse(null);
        boolean exhausted = "0".equals(remaining);
        boolean rateLimited = status == 429
                || exhausted
                || response.headers().firstValue("Retry-After").isPresent()
                || signalsRateLimit(providerMessage);
        if (!rateLimited) return null;

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
        if (retryAfter != null) {
            try { return now.plusSeconds(Math.max(1, Long.parseLong(retryAfter.trim()))); }
            catch (NumberFormatException ignored) { }
        }
        String reset = response.headers().firstValue("X-RateLimit-Reset").orElse(null);
        if (reset != null) {
            try {
                OffsetDateTime value = OffsetDateTime.ofInstant(
                        java.time.Instant.ofEpochSecond(Long.parseLong(reset.trim())), ZoneOffset.UTC);
                if (value.isAfter(now)) return value.plusSeconds(1);
            } catch (NumberFormatException ignored) { }
        }
        return now.plusMinutes(1);
    }

    private String graphqlErrorMessage(String body) {
        try {
            JsonNode json = mapper.readTree(body);
            JsonNode errors = json.path("errors");
            if (errors.isArray() && !errors.isEmpty()) {
                String message = errors.get(0).path("message").asText(null);
                if (message != null && !message.isBlank()) return message;
            }
            String message = json.path("message").asText(null);
            return message == null || message.isBlank() ? null : message;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean signalsRateLimit(String message) {
        if (message == null) return false;
        String normalized = message.toLowerCase(Locale.ROOT);
        return normalized.contains("rate limit") || normalized.contains("secondary rate");
    }

    private OffsetDateTime parseDate(JsonNode node, String field) {
        if (!node.hasNonNull(field)) return null;
        try { return OffsetDateTime.parse(node.get(field).asText()); }
        catch (RuntimeException ignored) { return null; }
    }

    private String query() {
        return """
                query($owner: String!, $name: String!, $login: String!, $cursor: String) {
                  repository(owner: $owner, name: $name) {
                    pullRequests(first: %d, after: $cursor, states: [OPEN, CLOSED, MERGED], orderBy: {field: UPDATED_AT, direction: DESC}) {
                      nodes {
                        number
                        title
                        updatedAt
                        reviews(first: 100, author: $login) {
                          nodes {
                            databaseId
                            body
                            state
                            submittedAt
                          }
                        }
                      }
                      pageInfo {
                        hasNextPage
                        endCursor
                      }
                    }
                  }
                }
                """.formatted(PULL_REQUEST_PAGE_SIZE);
    }
}
