package io.github.developeranalytics.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.observability.StructuredLog;
import io.github.developeranalytics.provider.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@ApplicationScoped
public class GitHubProviderAdapter implements SourceControlProvider {

    private static final Logger LOG = Logger.getLogger(GitHubProviderAdapter.class);
    static final String API_BASE = "https://api.github.com";
    static final String API_VERSION = "2022-11-28";
    static final int PAGE_SIZE = 100;

    @Inject ObjectMapper mapper;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override public String providerKey() { return "github"; }

    @Override
    public ProviderUser fetchCurrentUser(ProviderAccessToken accessToken) throws ProviderException {
        HttpResponse<String> response = sendGet(URI.create(API_BASE + "/user"), accessToken);
        JsonNode json = parse(response.body());
        return new ProviderUser(json.get("id").asText(), json.get("login").asText(),
                json.hasNonNull("name") ? json.get("name").asText() : null);
    }

    @Override
    public PagedResult<ProviderRepository> listRepositories(ProviderAccessToken accessToken, String pageCursor) throws ProviderException {
        int page = parsePage(pageCursor);
        URI uri = URI.create(API_BASE + "/user/repos?affiliation=owner%2Ccollaborator%2Corganization_member" +
                "&visibility=all&sort=updated&direction=desc&per_page=" + PAGE_SIZE + "&page=" + page);
        HttpResponse<String> response = sendGet(uri, accessToken);
        JsonNode array = parse(response.body());
        if (!array.isArray()) throw new ProviderException("GitHub repository response was not an array", response.statusCode());
        List<ProviderRepository> repositories = new ArrayList<>();
        for (JsonNode node : array) repositories.add(mapRepository(node));
        String nextCursor = repositories.size() == PAGE_SIZE ? Integer.toString(page + 1) : null;
        return new PagedResult<>(repositories, nextCursor, parseRateLimit(response));
    }

    @Override
    public ProviderRepositorySnapshot fetchRepositorySnapshot(ProviderAccessToken accessToken, ProviderRepository repository) throws ProviderException {
        String fullName = repository.fullName();
        if (fullName == null || !fullName.contains("/")) throw new ProviderException("GitHub repository full name is required", 0);
        HttpResponse<String> metadataResponse = sendGet(URI.create(API_BASE + "/repos/" + fullName), accessToken);
        JsonNode metadata = parse(metadataResponse.body());
        String defaultBranch = metadata.hasNonNull("default_branch") ? metadata.get("default_branch").asText() : null;
        if (defaultBranch == null || defaultBranch.isBlank()) return new ProviderRepositorySnapshot(List.of(), parseRateLimit(metadataResponse));

        HttpResponse<String> treeResponse = sendGet(URI.create(API_BASE + "/repos/" + fullName + "/git/trees/" +
                java.net.URLEncoder.encode(defaultBranch, java.nio.charset.StandardCharsets.UTF_8) + "?recursive=1"), accessToken);
        JsonNode tree = parse(treeResponse.body()).path("tree");
        if (!tree.isArray()) throw new ProviderException("GitHub repository tree response was not an array", treeResponse.statusCode());

        List<String> relevantPaths = new ArrayList<>();
        for (JsonNode item : tree) {
            if (!"blob".equals(item.path("type").asText())) continue;
            String path = item.path("path").asText("");
            if (isRelevantTechnologyFile(path)) {
                relevantPaths.add(path);
                if (relevantPaths.size() >= 40) break;
            }
        }

        List<ProviderRepositoryFile> files = new ArrayList<>();
        for (String path : relevantPaths) {
            URI contentUri = URI.create(API_BASE + "/repos/" + fullName + "/contents/" + encodePath(path) + "?ref=" +
                    java.net.URLEncoder.encode(defaultBranch, java.nio.charset.StandardCharsets.UTF_8));
            HttpResponse<String> contentResponse = sendGet(contentUri, accessToken);
            JsonNode contentJson = parse(contentResponse.body());
            String encoding = contentJson.path("encoding").asText("");
            String content = contentJson.path("content").asText("");
            if (!"base64".equalsIgnoreCase(encoding) || content.isBlank()) continue;
            try {
                byte[] decoded = Base64.getMimeDecoder().decode(content);
                files.add(new ProviderRepositoryFile(path, new String(decoded, java.nio.charset.StandardCharsets.UTF_8)));
            } catch (IllegalArgumentException ignored) {
                // Skip malformed file payloads.
            }
        }
        return new ProviderRepositorySnapshot(files, parseRateLimit(treeResponse));
    }

    private boolean isRelevantTechnologyFile(String rawPath) {
        String path = rawPath.toLowerCase(Locale.ROOT);
        String name = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        return name.equals("pom.xml") || name.equals("package.json") || name.equals("dockerfile") ||
                name.equals("docker-compose.yml") || name.equals("docker-compose.yaml") || name.equals("compose.yml") ||
                name.equals("compose.yaml") || name.equals("package.swift") || name.equals("pyproject.toml") ||
                name.equals("requirements.txt") || name.equals(".terraform.lock.hcl") || name.endsWith(".tf") ||
                name.equals("chart.yaml") || name.equals("kustomization.yaml") || name.equals("androidmanifest.xml") ||
                name.equals("project.pbxproj") || name.equals("platformio.ini") || name.endsWith(".ino") ||
                path.startsWith(".github/workflows/") || path.contains("/.github/workflows/") ||
                path.startsWith("db/migration/") || path.contains("/db/migration/");
    }

    private String encodePath(String path) {
        return Arrays.stream(path.split("/"))
                .map(segment -> java.net.URLEncoder.encode(segment, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20"))
                .collect(java.util.stream.Collectors.joining("/"));
    }

    @Override
    public ProviderLanguageBreakdown fetchRepositoryLanguages(ProviderAccessToken accessToken, ProviderRepository repository) throws ProviderException {
        String fullName = repository.fullName();
        if (fullName == null || !fullName.contains("/")) throw new ProviderException("GitHub repository full name is required", 0);
        HttpResponse<String> response = sendGet(URI.create(API_BASE + "/repos/" + fullName + "/languages"), accessToken);
        JsonNode object = parse(response.body());
        if (!object.isObject()) throw new ProviderException("GitHub languages response was not an object", response.statusCode());
        Map<String, Long> languages = new LinkedHashMap<>();
        object.fields().forEachRemaining(entry -> languages.put(entry.getKey(), entry.getValue().asLong()));
        return new ProviderLanguageBreakdown(languages, parseRateLimit(response));
    }

    @Override
    public PagedResult<ProviderContribution> listContributions(ProviderAccessToken accessToken, ProviderRepository repository,
                                                               OffsetDateTime since, String pageCursor) throws ProviderException {
        return listContributions(accessToken, repository, since, pageCursor, null);
    }

    public PagedResult<ProviderContribution> listContributions(ProviderAccessToken accessToken, ProviderRepository repository,
                                                               OffsetDateTime since, String pageCursor, String userLogin) throws ProviderException {
        int page = parsePage(pageCursor);
        String fullName = repository.fullName();
        if (fullName == null || !fullName.contains("/")) throw new ProviderException("GitHub repository full name is required", 0);
        List<ProviderContribution> contributions = new ArrayList<>();

        URI commitsUri = URI.create(API_BASE + "/repos/" + fullName + "/commits?per_page=" + PAGE_SIZE + "&page=" + page +
                (userLogin == null || userLogin.isBlank() ? "" : "&author=" + java.net.URLEncoder.encode(userLogin, java.nio.charset.StandardCharsets.UTF_8)) +
                (since == null ? "" : "&since=" + java.net.URLEncoder.encode(since.toString(), java.nio.charset.StandardCharsets.UTF_8)));
        HttpResponse<String> commitsResponse = sendGet(commitsUri, accessToken);
        JsonNode commitsArray = parse(commitsResponse.body());
        if (!commitsArray.isArray()) throw new ProviderException("GitHub commit response was not an array", commitsResponse.statusCode());

        for (JsonNode node : commitsArray) {
            JsonNode commit = node.path("commit");
            JsonNode author = commit.path("author");
            contributions.add(new ProviderContribution(node.path("sha").asText(), ProviderContribution.Type.COMMIT,
                    firstLine(commit.path("message").asText(null)),
                    author.hasNonNull("date") ? OffsetDateTime.parse(author.get("date").asText()) : null,
                    ProviderContribution.State.UNKNOWN, null, null, null, null));
        }

        if (page == 1) {
            JsonNode pullsArray = optionalPullRequests(fullName, accessToken);
            for (JsonNode node : pullsArray) {
                if (userLogin != null && !userLogin.isBlank() && !userLogin.equalsIgnoreCase(node.path("user").path("login").asText(""))) continue;
                OffsetDateTime updatedAt = parseNullableDate(node, "updated_at");
                if (since != null && updatedAt != null && updatedAt.isBefore(since)) continue;
                boolean merged = node.hasNonNull("merged_at");
                ProviderContribution.State state = merged ? ProviderContribution.State.MERGED :
                        ("open".equals(node.path("state").asText()) ? ProviderContribution.State.OPEN : ProviderContribution.State.CLOSED);
                String pullId = node.path("id").asText();
                int pullNumber = node.path("number").asInt();
                contributions.add(new ProviderContribution("pr-" + pullId, ProviderContribution.Type.PULL_REQUEST,
                        node.path("title").asText(null), updatedAt, state, null, null, null, merged));
                contributions.addAll(optionalReviews(fullName, pullNumber, accessToken, since, userLogin));
            }
            contributions.addAll(optionalIssues(fullName, accessToken, since, userLogin));
        }

        String nextCursor = commitsArray.size() == PAGE_SIZE ? Integer.toString(page + 1) : null;
        return new PagedResult<>(contributions, nextCursor, parseRateLimit(commitsResponse));
    }

    private JsonNode optionalPullRequests(String fullName, ProviderAccessToken token) throws ProviderException {
        try { return fetchPullRequests(fullName, token); }
        catch (ProviderException e) {
            if (!isOptionalPermissionFailure(e)) throw e;
            logOptionalContributionFailure("pulls", e);
            return mapper.createArrayNode();
        }
    }

    private List<ProviderContribution> optionalReviews(String fullName, int pullNumber, ProviderAccessToken token,
                                                        OffsetDateTime since, String userLogin) throws ProviderException {
        try { return fetchReviews(fullName, pullNumber, token, since, userLogin); }
        catch (ProviderException e) {
            if (!isOptionalPermissionFailure(e)) throw e;
            logOptionalContributionFailure("reviews", e);
            return List.of();
        }
    }

    private List<ProviderContribution> optionalIssues(String fullName, ProviderAccessToken token,
                                                       OffsetDateTime since, String userLogin) throws ProviderException {
        try { return fetchIssues(fullName, token, since, userLogin); }
        catch (ProviderException e) {
            if (!isOptionalPermissionFailure(e)) throw e;
            logOptionalContributionFailure("issues", e);
            return List.of();
        }
    }

    private boolean isOptionalPermissionFailure(ProviderException e) {
        if (e.getStatusCode() != 403 && e.getStatusCode() != 404) return false;
        String message = e.getMessage() == null ? "" : e.getMessage().toLowerCase(Locale.ROOT);
        return !message.contains("rate limit") && !message.contains("secondary rate");
    }

    private void logOptionalContributionFailure(String endpoint, ProviderException e) {
        StructuredLog.warn(LOG, "github_optional_contribution_endpoint_unavailable", e,
                StructuredLog.fields("endpoint", endpoint, "httpStatus", e.getStatusCode()));
    }

    private JsonNode fetchPullRequests(String fullName, ProviderAccessToken accessToken) throws ProviderException {
        HttpResponse<String> response = sendGet(URI.create(API_BASE + "/repos/" + fullName +
                "/pulls?state=all&sort=updated&direction=desc&per_page=" + PAGE_SIZE), accessToken);
        JsonNode array = parse(response.body());
        if (!array.isArray()) throw new ProviderException("GitHub pull request response was not an array", response.statusCode());
        return array;
    }

    private List<ProviderContribution> fetchReviews(String fullName, int pullNumber, ProviderAccessToken accessToken,
                                                     OffsetDateTime since, String userLogin) throws ProviderException {
        HttpResponse<String> response = sendGet(URI.create(API_BASE + "/repos/" + fullName + "/pulls/" + pullNumber +
                "/reviews?per_page=" + PAGE_SIZE), accessToken);
        JsonNode array = parse(response.body());
        if (!array.isArray()) throw new ProviderException("GitHub review response was not an array", response.statusCode());
        List<ProviderContribution> result = new ArrayList<>();
        for (JsonNode node : array) {
            if (userLogin != null && !userLogin.isBlank() && !userLogin.equalsIgnoreCase(node.path("user").path("login").asText(""))) continue;
            OffsetDateTime submittedAt = parseNullableDate(node, "submitted_at");
            if (since != null && submittedAt != null && submittedAt.isBefore(since)) continue;
            ProviderContribution.State state = "DISMISSED".equalsIgnoreCase(node.path("state").asText(""))
                    ? ProviderContribution.State.CLOSED : ProviderContribution.State.UNKNOWN;
            String body = node.hasNonNull("body") ? node.get("body").asText() : null;
            String title = body == null || body.isBlank() ? "Pull request review" : body;
            result.add(new ProviderContribution("review-" + node.path("id").asText(), ProviderContribution.Type.REVIEW,
                    title, submittedAt, state, null, null, null, null));
        }
        return result;
    }

    private List<ProviderContribution> fetchIssues(String fullName, ProviderAccessToken accessToken,
                                                    OffsetDateTime since, String userLogin) throws ProviderException {
        String sinceQuery = since == null ? "" : "&since=" + java.net.URLEncoder.encode(since.toString(), java.nio.charset.StandardCharsets.UTF_8);
        HttpResponse<String> response = sendGet(URI.create(API_BASE + "/repos/" + fullName +
                "/issues?state=all&sort=updated&direction=desc&per_page=" + PAGE_SIZE + sinceQuery), accessToken);
        JsonNode array = parse(response.body());
        if (!array.isArray()) throw new ProviderException("GitHub issue response was not an array", response.statusCode());
        List<ProviderContribution> result = new ArrayList<>();
        for (JsonNode node : array) {
            if (node.has("pull_request")) continue;
            if (userLogin != null && !userLogin.isBlank() && !userLogin.equalsIgnoreCase(node.path("user").path("login").asText(""))) continue;
            OffsetDateTime updatedAt = parseNullableDate(node, "updated_at");
            if (since != null && updatedAt != null && updatedAt.isBefore(since)) continue;
            ProviderContribution.State state = "open".equals(node.path("state").asText())
                    ? ProviderContribution.State.OPEN : ProviderContribution.State.CLOSED;
            result.add(new ProviderContribution("issue-" + node.path("id").asText(), ProviderContribution.Type.ISSUE,
                    node.path("title").asText(null), updatedAt, state, null, null, null, null));
        }
        return result;
    }

    private String firstLine(String value) {
        if (value == null) return null;
        String normalized = value.replace("\r\n", "\n").replace('\r', '\n');
        int newline = normalized.indexOf('\n');
        return (newline >= 0 ? normalized.substring(0, newline) : normalized).strip();
    }

    private OffsetDateTime parseNullableDate(JsonNode node, String field) {
        return node.hasNonNull(field) ? OffsetDateTime.parse(node.get(field).asText()) : null;
    }

    public ProviderContributorStatistics fetchContributorStatistics(ProviderAccessToken accessToken,
                                                                    ProviderRepository repository,
                                                                    String userLogin) throws ProviderException {
        String fullName = repository.fullName();
        if (fullName == null || !fullName.contains("/")) throw new ProviderException("GitHub repository full name is required", 0);
        HttpResponse<String> response = sendGet(URI.create(API_BASE + "/repos/" + fullName + "/stats/contributors"), accessToken);
        JsonNode array = parse(response.body());
        if (!array.isArray()) throw new ProviderException("GitHub contributor statistics response was not an array", response.statusCode());
        int contributors = 0, humans = 0, bots = 0, userCommits = 0, repositoryCommits = 0;
        long userAdditions = 0, userDeletions = 0;
        for (JsonNode node : array) {
            JsonNode author = node.path("author");
            String login = author.path("login").asText("");
            boolean bot = "Bot".equalsIgnoreCase(author.path("type").asText("")) || login.toLowerCase(Locale.ROOT).endsWith("[bot]");
            contributors++;
            repositoryCommits += node.path("total").asInt(0);
            if (bot) bots++; else humans++;
            if (userLogin != null && userLogin.equalsIgnoreCase(login)) {
                userCommits += node.path("total").asInt(0);
                for (JsonNode week : node.path("weeks")) {
                    userAdditions += week.path("a").asLong(0);
                    userDeletions += week.path("d").asLong(0);
                }
            }
        }
        return new ProviderContributorStatistics(contributors, humans, bots, userCommits, repositoryCommits,
                userAdditions, userDeletions, OffsetDateTime.now(ZoneOffset.UTC));
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
                String providerMessage = githubErrorMessage(response.body());
                String sso = response.headers().firstValue("X-GitHub-SSO").orElse(null);
                String message = "GitHub API " + endpointLabel(uri) + " failed with HTTP " + response.statusCode();
                if (providerMessage != null) message += ": " + providerMessage;
                if (sso != null && !sso.isBlank()) message += " (GitHub SSO: " + sso + ")";
                throw new ProviderException(message, response.statusCode());
            }
            return response;
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderException("GitHub API " + endpointLabel(uri) + " request failed", 0, e);
        }
    }

    private String githubErrorMessage(String body) {
        try {
            JsonNode json = mapper.readTree(body);
            String message = json.path("message").asText(null);
            return message == null || message.isBlank() ? null : message.replaceAll("\\s+", " ").strip();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String endpointLabel(URI uri) {
        String path = uri.getPath();
        if (path.endsWith("/commits")) return "commits";
        if (path.endsWith("/pulls")) return "pulls";
        if (path.matches(".*/pulls/[^/]+/reviews$")) return "reviews";
        if (path.endsWith("/issues")) return "issues";
        if (path.endsWith("/stats/contributors")) return "contributor-statistics";
        if (path.endsWith("/languages")) return "languages";
        if (path.contains("/git/trees/")) return "repository-tree";
        if (path.contains("/contents/")) return "repository-content";
        if (path.equals("/user/repos")) return "repositories";
        if (path.equals("/user")) return "current-user";
        if (path.startsWith("/repos/")) return "repository";
        return "request";
    }

    JsonNode parse(String body) throws ProviderException {
        try { return mapper.readTree(body); }
        catch (Exception e) { throw new ProviderException("Could not parse GitHub API response", 0, e); }
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
        return new ProviderRepository(node.get("id").asText(),
                owner != null && owner.hasNonNull("id") ? owner.get("id").asText() : null,
                owner != null && owner.hasNonNull("login") ? owner.get("login").asText() : null,
                ownerType, node.get("name").asText(), node.hasNonNull("full_name") ? node.get("full_name").asText() : null,
                node.hasNonNull("html_url") ? node.get("html_url").asText() : null,
                isPrivate ? ProviderRepository.Visibility.PRIVATE : ProviderRepository.Visibility.PUBLIC,
                node.path("fork").asBoolean(false), node.path("archived").asBoolean(false),
                parseDate(node, "created_at"), parseDate(node, "updated_at"), parseDate(node, "pushed_at"),
                node.hasNonNull("description") ? node.get("description").asText() : null, mapTopics(node));
    }

    private List<String> mapTopics(JsonNode node) {
        JsonNode topics = node.path("topics");
        if (!topics.isArray()) return List.of();
        List<String> result = new ArrayList<>();
        for (JsonNode topic : topics) if (topic.isTextual() && !topic.asText().isBlank()) result.add(topic.asText());
        return result;
    }

    ProviderRateLimit parseRateLimit(HttpResponse<?> response) {
        Integer limit = parseIntegerHeader(response, "X-RateLimit-Limit");
        Integer remaining = parseIntegerHeader(response, "X-RateLimit-Remaining");
        OffsetDateTime resetAt = response.headers().firstValue("X-RateLimit-Reset").flatMap(this::parseEpochSeconds).orElse(null);
        return new ProviderRateLimit(limit, remaining, resetAt);
    }

    private Optional<OffsetDateTime> parseEpochSeconds(String raw) {
        try { return Optional.of(OffsetDateTime.ofInstant(Instant.ofEpochSecond(Long.parseLong(raw)), ZoneOffset.UTC)); }
        catch (RuntimeException e) { return Optional.empty(); }
    }

    private Integer parseIntegerHeader(HttpResponse<?> response, String name) {
        return response.headers().firstValue(name).flatMap(value -> {
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
