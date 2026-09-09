package io.github.developeranalytics.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderContributorActivityWeek;
import io.github.developeranalytics.provider.ProviderContributorSnapshot;
import io.github.developeranalytics.provider.ProviderContributorStatistics;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Fetches GitHub contributor totals and weekly user activity from one stats response. */
@ApplicationScoped
public class GitHubContributorSnapshotService {

    @Inject GitHubProviderAdapter github;
    @Inject ObjectMapper mapper;

    public ProviderContributorSnapshot fetch(
            ProviderAccessToken accessToken,
            ProviderRepository repository,
            String userLogin
    ) throws ProviderException {
        String fullName = repository.fullName();
        if (fullName == null || !fullName.contains("/")) {
            throw new ProviderException("GitHub repository full name is required", 0);
        }

        HttpResponse<String> response = github.sendGet(
                URI.create(GitHubProviderAdapter.API_BASE + "/repos/" + fullName + "/stats/contributors"),
                accessToken
        );
        JsonNode array = parse(response.body());
        if (!array.isArray()) {
            throw new ProviderException("GitHub contributor statistics response was not an array", response.statusCode());
        }

        int contributors = 0;
        int humans = 0;
        int bots = 0;
        int userCommits = 0;
        int repositoryCommits = 0;
        long userAdditions = 0;
        long userDeletions = 0;
        List<ProviderContributorActivityWeek> userWeeks = new ArrayList<>();

        for (JsonNode node : array) {
            JsonNode author = node.path("author");
            String login = author.path("login").asText("");
            boolean bot = "Bot".equalsIgnoreCase(author.path("type").asText(""))
                    || login.toLowerCase(Locale.ROOT).endsWith("[bot]");

            contributors++;
            repositoryCommits += node.path("total").asInt(0);
            if (bot) bots++; else humans++;

            if (userLogin == null || !userLogin.equalsIgnoreCase(login)) continue;

            userCommits += node.path("total").asInt(0);
            for (JsonNode week : node.path("weeks")) {
                int commits = week.path("c").asInt(0);
                long additions = week.path("a").asLong(0);
                long deletions = week.path("d").asLong(0);
                userAdditions += additions;
                userDeletions += deletions;

                long epochSeconds = week.path("w").asLong(0);
                if (epochSeconds <= 0 || (commits == 0 && additions == 0 && deletions == 0)) continue;
                userWeeks.add(new ProviderContributorActivityWeek(
                        Instant.ofEpochSecond(epochSeconds).atOffset(ZoneOffset.UTC).toLocalDate(),
                        commits,
                        additions,
                        deletions
                ));
            }
        }

        ProviderContributorStatistics statistics = new ProviderContributorStatistics(
                contributors,
                humans,
                bots,
                userCommits,
                repositoryCommits,
                userAdditions,
                userDeletions,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        return new ProviderContributorSnapshot(statistics, userWeeks);
    }

    private JsonNode parse(String body) throws ProviderException {
        try {
            return mapper.readTree(body);
        } catch (Exception e) {
            throw new ProviderException("Could not parse GitHub contributor statistics response", 0, e);
        }
    }
}
