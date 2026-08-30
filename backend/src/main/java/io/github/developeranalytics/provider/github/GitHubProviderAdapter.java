package io.github.developeranalytics.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.provider.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@ApplicationScoped
public class GitHubProviderAdapter implements SourceControlProvider {

    static final String API_BASE = "https://api.github.com";
    static final String API_VERSION = "2022-11-28";
    static final int PAGE_SIZE = 100;

    @Inject
    ObjectMapper mapper;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String providerKey() {
        return "github";
    }

    @Override
    public ProviderUser fetchCurrentUser(ProviderAccessToken accessToken) throws ProviderException {
        HttpResponse<String> response = sendGet(URI.create(API_BASE + "/user"), accessToken);
        JsonNode json = parse(response.body());
        return new ProviderUser(
                json.get("id").asText(),
                json.get("login").asText(),
                json.hasNonNull("name") ? json.get("name").asText() : null
        );
    }

    @Override
    public PagedResult<ProviderRepository> listRepositories(
            ProviderAccessToken accessToken,
            String pageCursor
    ) throws ProviderException {
        int page = parsePage(pageCursor);
        URI uri = URI.create(
                API_BASE + "/user/repos" +
                "?affiliation=owner%2Ccollaborator%2Corganization_member" +
                "&visibility=all&sort=updated&direction=desc" +
                "&per_page=" + PAGE_SIZE + "&page=" + page
        );

        HttpResponse<String> response = sendGet(uri, accessToken);
        JsonNode array = parse(response.body());
        if (!array.isArray()) {
            throw new ProviderException("GitHub repository response was not an array", response.statusCode());
        }

        List<ProviderRepository> repositories = new ArrayList<>();
        for (JsonNode node : array) repositories.add(mapRepository(node));

        String nextCursor = repositories.size() == PAGE_SIZE ? Integer.toString(page + 1) : null;
        return new PagedResult<>(repositories, nextCursor, parseRateLimit(response));
    }




@Override
public ProviderLanguageBreakdown fetchRepositoryLanguages(
        ProviderAccessToken accessToken,
        ProviderRepository repository
) throws ProviderException {
    String fullName = repository.fullName();
    if (fullName == null || !fullName.contains("/")) {
        throw new ProviderException("GitHub repository full name is required", 0);
    }

    HttpResponse<String> response = sendGet(
            URI.create(API_BASE + "/repos/" + fullName + "/languages"),
            accessToken
    );

    JsonNode object = parse(response.body());
    if (!object.isObject()) {
        throw new ProviderException(
                "GitHub languages response was not an object",
                response.statusCode()
        );
    }

    Map<String, Long> languages = new LinkedHashMap<>();
    object.fields().forEachRemaining(entry ->
            languages.put(entry.getKey(), entry.getValue().asLong())
    );

    return new ProviderLanguageBreakdown(
            languages,
            parseRateLimit(response)
    );
}

@Override
public PagedResult<ProviderContribution> listContributions(
        ProviderAccessToken accessToken,
        ProviderRepository repository,
        OffsetDateTime since,
        String pageCursor
) throws ProviderException {
    int page = parsePage(pageCursor);
    String fullName = repository.fullName();
    if (fullName == null || !fullName.contains("/")) {
        throw new ProviderException("GitHub repository full name is required", 0);
    }

    List<ProviderContribution> contributions = new ArrayList<>();

    URI commitsUri = URI.create(
            API_BASE + "/repos/" + fullName + "/commits" +
            "?per_page=" + PAGE_SIZE + "&page=" + page +
            (since == null ? "" : "&since=" + java.net.URLEncoder.encode(
                    since.toString(), java.nio.charset.StandardCharsets.UTF_8))
    );

    HttpResponse<String> commitsResponse = sendGet(commitsUri, accessToken);
    JsonNode commitsArray = parse(commitsResponse.body());
    if (!commitsArray.isArray()) {
        throw new ProviderException(
                "GitHub commit response was not an array",
                commitsResponse.statusCode()
        );
    }

    for (JsonNode node : commitsArray) {
        JsonNode commit = node.path("commit");
        JsonNode author = commit.path("author");
        contributions.add(new ProviderContribution(
                node.path("sha").asText(),
                ProviderContribution.Type.COMMIT,
                commit.path("message").asText(null),
                author.hasNonNull("date")
                        ? OffsetDateTime.parse(author.get("date").asText())
                        : null,
                ProviderContribution.State.UNKNOWN,
                null,
                null,
                null,
                null
        ));
    }

    // Refresh recent PRs, reviews and issues once per contribution sync.
    if (page == 1) {
        JsonNode pullsArray = fetchPullRequests(fullName, accessToken);

        for (JsonNode node : pullsArray) {
            OffsetDateTime updatedAt = parseNullableDate(node, "updated_at");
            if (since != null && updatedAt != null && updatedAt.isBefore(since)) {
                continue;
            }

            boolean merged = node.hasNonNull("merged_at");
            ProviderContribution.State state = merged
                    ? ProviderContribution.State.MERGED
                    : ("open".equals(node.path("state").asText())
                        ? ProviderContribution.State.OPEN
                        : ProviderContribution.State.CLOSED);

            String pullId = node.path("id").asText();
            int pullNumber = node.path("number").asInt();

            contributions.add(new ProviderContribution(
                    "pr-" + pullId,
                    ProviderContribution.Type.PULL_REQUEST,
                    node.path("title").asText(null),
                    updatedAt,
                    state,
                    null,
                    null,
                    null,
                    merged
            ));

            contributions.addAll(fetchReviews(
                    fullName,
                    pullNumber,
                    accessToken,
                    since
            ));
        }

        contributions.addAll(fetchIssues(fullName, accessToken, since));
    }

    String nextCursor = commitsArray.size() == PAGE_SIZE
            ? Integer.toString(page + 1)
            : null;

    return new PagedResult<>(
            contributions,
            nextCursor,
            parseRateLimit(commitsResponse)
    );
}

private JsonNode fetchPullRequests(
        String fullName,
        ProviderAccessToken accessToken
) throws ProviderException {
    URI pullsUri = URI.create(
            API_BASE + "/repos/" + fullName +
            "/pulls?state=all&sort=updated&direction=desc&per_page=" + PAGE_SIZE
    );

    HttpResponse<String> response = sendGet(pullsUri, accessToken);
    JsonNode array = parse(response.body());
    if (!array.isArray()) {
        throw new ProviderException(
                "GitHub pull request response was not an array",
                response.statusCode()
        );
    }
    return array;
}

private List<ProviderContribution> fetchReviews(
        String fullName,
        int pullNumber,
        ProviderAccessToken accessToken,
        OffsetDateTime since
) throws ProviderException {
    URI reviewsUri = URI.create(
            API_BASE + "/repos/" + fullName + "/pulls/" +
            pullNumber + "/reviews?per_page=" + PAGE_SIZE
    );

    HttpResponse<String> response = sendGet(reviewsUri, accessToken);
    JsonNode array = parse(response.body());
    if (!array.isArray()) {
        throw new ProviderException(
                "GitHub review response was not an array",
                response.statusCode()
        );
    }

    List<ProviderContribution> result = new ArrayList<>();
    for (JsonNode node : array) {
        OffsetDateTime submittedAt = parseNullableDate(node, "submitted_at");
        if (since != null && submittedAt != null && submittedAt.isBefore(since)) {
            continue;
        }

        ProviderContribution.State state =
                "DISMISSED".equalsIgnoreCase(node.path("state").asText(""))
                        ? ProviderContribution.State.CLOSED
                        : ProviderContribution.State.UNKNOWN;

        String body = node.hasNonNull("body") ? node.get("body").asText() : null;
        String title = body == null || body.isBlank()
                ? "Pull request review"
                : body;

        result.add(new ProviderContribution(
                "review-" + node.path("id").asText(),
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

private List<ProviderContribution> fetchIssues(
        String fullName,
        ProviderAccessToken accessToken,
        OffsetDateTime since
) throws ProviderException {
    String sinceQuery = since == null
            ? ""
            : "&since=" + java.net.URLEncoder.encode(
                    since.toString(),
                    java.nio.charset.StandardCharsets.UTF_8
            );

    URI issuesUri = URI.create(
            API_BASE + "/repos/" + fullName +
            "/issues?state=all&sort=updated&direction=desc&per_page=" +
            PAGE_SIZE + sinceQuery
    );

    HttpResponse<String> response = sendGet(issuesUri, accessToken);
    JsonNode array = parse(response.body());
    if (!array.isArray()) {
        throw new ProviderException(
                "GitHub issue response was not an array",
                response.statusCode()
        );
    }

    List<ProviderContribution> result = new ArrayList<>();
    for (JsonNode node : array) {
        // GitHub's issues API also includes pull requests.
        if (node.has("pull_request")) {
            continue;
        }

        OffsetDateTime updatedAt = parseNullableDate(node, "updated_at");
        if (since != null && updatedAt != null && updatedAt.isBefore(since)) {
            continue;
        }

        ProviderContribution.State state =
                "open".equals(node.path("state").asText())
                        ? ProviderContribution.State.OPEN
                        : ProviderContribution.State.CLOSED;

        result.add(new ProviderContribution(
                "issue-" + node.path("id").asText(),
                ProviderContribution.Type.ISSUE,
                node.path("title").asText(null),
                updatedAt,
                state,
                null,
                null,
                null,
                null
        ));
    }

    return result;
}

private OffsetDateTime parseNullableDate(JsonNode node, String field) {
    return node.hasNonNull(field)
            ? OffsetDateTime.parse(node.get(field).asText())
            : null;
}

HttpResponse<String> sendGet(URI uri, ProviderAccessToken accessToken) throws ProviderException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Accept", "application/vnd.github+json")
                .header("Authorization", "Bearer " + accessToken.value())
                .header("X-GitHub-Api-Version", API_VERSION)
                .header("User-Agent", "developer-analytics")
                .GET().build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                throw new ProviderException(
                        "GitHub API request failed with HTTP " + response.statusCode(),
                        response.statusCode());
            }
            return response;
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderException("GitHub API request failed", 0, e);
        }
    }

    JsonNode parse(String body) throws ProviderException {
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            throw new ProviderException("Could not parse GitHub API response", 0, e);
        }
    }

    ProviderRepository mapRepository(JsonNode node) {
        JsonNode owner = node.get("owner");
        String ownerTypeText = owner != null && owner.hasNonNull("type") ? owner.get("type").asText() : "";

        ProviderRepository.OwnerType ownerType = switch (ownerTypeText) {
            case "User" -> ProviderRepository.OwnerType.USER;
            case "Organization" -> ProviderRepository.OwnerType.ORGANIZATION;
            default -> ProviderRepository.OwnerType.OTHER;
        };

        boolean isPrivate = node.path("private").asBoolean(false);

        return new ProviderRepository(
                node.get("id").asText(),
                owner != null && owner.hasNonNull("id") ? owner.get("id").asText() : null,
                owner != null && owner.hasNonNull("login") ? owner.get("login").asText() : null,
                ownerType,
                node.get("name").asText(),
                node.hasNonNull("full_name") ? node.get("full_name").asText() : null,
                node.hasNonNull("html_url") ? node.get("html_url").asText() : null,
                isPrivate ? ProviderRepository.Visibility.PRIVATE : ProviderRepository.Visibility.PUBLIC,
                node.path("fork").asBoolean(false),
                node.path("archived").asBoolean(false),
                parseDate(node, "created_at"),
                parseDate(node, "updated_at"),
                parseDate(node, "pushed_at"),
                node.hasNonNull("description") ? node.get("description").asText() : null,
                mapTopics(node)
        );
    }


private List<String> mapTopics(JsonNode node) {
    JsonNode topics = node.path("topics");
    if (!topics.isArray()) return List.of();

    List<String> result = new ArrayList<>();
    for (JsonNode topic : topics) {
        if (topic.isTextual() && !topic.asText().isBlank()) {
            result.add(topic.asText());
        }
    }
    return result;
}

    ProviderRateLimit parseRateLimit(HttpResponse<?> response) {
        Integer limit = parseIntegerHeader(response, "X-RateLimit-Limit");
        Integer remaining = parseIntegerHeader(response, "X-RateLimit-Remaining");
        OffsetDateTime resetAt = response.headers().firstValue("X-RateLimit-Reset")
                .flatMap(this::parseEpochSeconds).orElse(null);
        return new ProviderRateLimit(limit, remaining, resetAt);
    }

    private Optional<OffsetDateTime> parseEpochSeconds(String raw) {
        try {
            return Optional.of(OffsetDateTime.ofInstant(
                    Instant.ofEpochSecond(Long.parseLong(raw)), ZoneOffset.UTC));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private Integer parseIntegerHeader(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name)
                .flatMap(value -> {
                    try { return Optional.of(Integer.parseInt(value)); }
                    catch (NumberFormatException e) { return Optional.empty(); }
                }).orElse(null);
    }

    private int parsePage(String cursor) throws ProviderException {
        if (cursor == null || cursor.isBlank()) return 1;
        try {
            int page = Integer.parseInt(cursor);
            if (page < 1) throw new NumberFormatException();
            return page;
        } catch (NumberFormatException e) {
            throw new ProviderException("Invalid GitHub page cursor", 0, e);
        }
    }

    private OffsetDateTime parseDate(JsonNode node, String field) {
        return node.hasNonNull(field) ? OffsetDateTime.parse(node.get(field).asText()) : null;
    }
}
