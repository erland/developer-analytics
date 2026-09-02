package io.github.developeranalytics.service.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.observability.StructuredLog;
import io.github.developeranalytics.persistence.repository.RepositoryUserActivityWeekRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class GitHubWeeklyActivityService {
    private static final Logger LOG = Logger.getLogger(GitHubWeeklyActivityService.class);
    private static final String API_VERSION = "2022-11-28";

    @Inject ObjectMapper mapper;
    @Inject RepositoryUserActivityWeekRepository weeks;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Transactional
    public boolean refresh(UUID userId, SourceRepository repository,
                           ProviderAccessToken token, String userLogin) {
        if (repository.getFullName() == null || repository.getFullName().isBlank()
                || userLogin == null || userLogin.isBlank()) return false;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + repository.getFullName() + "/stats/contributors"))
                    .header("Authorization", "Bearer " + token.value())
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", API_VERSION)
                    .header("User-Agent", "developer-analytics")
                    .GET().build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 202) {
                StructuredLog.info(LOG, "weekly_activity_statistics_pending",
                        StructuredLog.fields("repositoryId", repository.getId()));
                return false;
            }
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.warn(StructuredLog.format("weekly_activity_statistics_unavailable",
                        StructuredLog.fields("repositoryId", repository.getId(), "httpStatus", response.statusCode())));
                return false;
            }

            JsonNode root = mapper.readTree(response.body());
            if (!root.isArray()) return false;
            List<RepositoryUserActivityWeekRepository.WeekInput> activity = new ArrayList<>();
            for (JsonNode contributor : root) {
                if (!userLogin.equalsIgnoreCase(contributor.path("author").path("login").asText(""))) continue;
                for (JsonNode week : contributor.path("weeks")) {
                    int commits = week.path("c").asInt(0);
                    long additions = week.path("a").asLong(0);
                    long deletions = week.path("d").asLong(0);
                    if (commits == 0 && additions == 0 && deletions == 0) continue;
                    long epochSeconds = week.path("w").asLong(0);
                    if (epochSeconds <= 0) continue;
                    activity.add(new RepositoryUserActivityWeekRepository.WeekInput(
                            Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.UTC).toLocalDate(),
                            commits, additions, deletions));
                }
                break;
            }
            weeks.replace(userId, repository.getId(), activity, OffsetDateTime.now(ZoneOffset.UTC));
            return true;
        } catch (Exception error) {
            StructuredLog.warn(LOG, "weekly_activity_statistics_unavailable", error,
                    StructuredLog.fields("repositoryId", repository.getId()));
            return false;
        }
    }
}
