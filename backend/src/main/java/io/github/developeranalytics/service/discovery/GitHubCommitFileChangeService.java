package io.github.developeranalytics.service.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.domain.change.ContributionFileChange;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.ContributionFileChangeRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@ApplicationScoped
public class GitHubCommitFileChangeService {
    private static final String API_VERSION = "2022-11-28";
    private static final int PAGE_SIZE = 100;

    @Inject ObjectMapper mapper;
    @Inject ChangeKindClassifier classifier;
    @Inject ContributionFileChangeRepository fileChanges;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public CommitDetails refresh(
            AppUser user,
            SourceRepository repository,
            Contribution contribution,
            ProviderAccessToken token
    ) throws ProviderException {
        if (repository.getFullName() == null || repository.getFullName().isBlank()) {
            throw new ProviderException("GitHub repository full name is required", 0);
        }

        fileChanges.deleteForContribution(contribution);

        int page = 1;
        int additions = 0;
        int deletions = 0;
        int changedFiles = 0;

        while (true) {
            JsonNode root = fetchCommitPage(repository.getFullName(), contribution.getProviderContributionId(), page, token);
            if (page == 1) {
                JsonNode stats = root.path("stats");
                additions = stats.path("additions").asInt(0);
                deletions = stats.path("deletions").asInt(0);
            }

            JsonNode files = root.path("files");
            if (!files.isArray()) {
                throw new ProviderException("GitHub commit files response was not an array", 0);
            }

            int pageCount = 0;
            for (JsonNode file : files) {
                String path = file.path("filename").asText(null);
                if (path == null || path.isBlank()) continue;
                int fileAdditions = Math.max(0, file.path("additions").asInt(0));
                int fileDeletions = Math.max(0, file.path("deletions").asInt(0));
                fileChanges.persist(new ContributionFileChange(
                        contribution,
                        user,
                        repository,
                        path,
                        fileAdditions,
                        fileDeletions,
                        classifier.classify(path),
                        contribution.getOccurredAt()
                ));
                changedFiles++;
                pageCount++;
            }

            if (pageCount < PAGE_SIZE) break;
            page++;
        }

        return new CommitDetails(additions, deletions, changedFiles);
    }

    private JsonNode fetchCommitPage(String fullName, String sha, int page, ProviderAccessToken token)
            throws ProviderException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.github.com/repos/" + fullName + "/commits/" + sha
                            + "?per_page=" + PAGE_SIZE + "&page=" + page))
                    .header("Authorization", "Bearer " + token.value())
                    .header("Accept", "application/vnd.github+json")
                    .header("X-GitHub-Api-Version", API_VERSION)
                    .header("User-Agent", "developer-analytics")
                    .GET()
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ProviderException("GitHub commit detail request failed", response.statusCode());
            }
            return mapper.readTree(response.body());
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderException("GitHub commit detail request failed", 0, e);
        }
    }

    public record CommitDetails(int additions, int deletions, int changedFiles) {}
}
