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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class GitHubCommitFileChangeService {
    private static final String API_VERSION = "2022-11-28";
    private static final int PAGE_SIZE = 100;
    private static final long SECONDARY_RATE_LIMIT_FALLBACK_SECONDS = 60;

    @Inject ObjectMapper mapper;
    @Inject ChangeKindClassifier classifier;
    @Inject ContributionFileChangeRepository fileChanges;

    private final HttpClient http = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public boolean hasCurrentClassification(Contribution contribution) {
        return fileChanges.hasCurrentClassification(contribution, ChangeKindClassifier.CLASSIFIER_VERSION);
    }

    public boolean hasMissingCurrentClassification(AppUser user, SourceRepository repository) {
        return fileChanges.hasMissingCurrentClassification(
                user.getId(), repository.getId(), ChangeKindClassifier.CLASSIFIER_VERSION);
    }

    public CommitDetails refresh(
            AppUser user,
            SourceRepository repository,
            Contribution contribution,
            ProviderAccessToken token
    ) throws ProviderException {
        if (repository.getFullName() == null || repository.getFullName().isBlank()) {
            throw new ProviderException("GitHub repository full name is required", 0);
        }

        int page = 1;
        int additions = 0;
        int deletions = 0;
        List<FileInput> fetchedFiles = new ArrayList<>();

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

            int returnedFileCount = files.size();
            for (JsonNode file : files) {
                String path = file.path("filename").asText(null);
                if (path == null || path.isBlank()) continue;
                fetchedFiles.add(new FileInput(
                        path,
                        Math.max(0, file.path("additions").asInt(0)),
                        Math.max(0, file.path("deletions").asInt(0))
                ));
            }

            if (returnedFileCount < PAGE_SIZE) break;
            page++;
        }

        // Replace only after every GitHub page has been fetched successfully. This keeps an
        // existing classification intact if GitHub rate-limits or fails part-way through.
        fileChanges.deleteForContribution(contribution);
        for (FileInput file : fetchedFiles) {
            fileChanges.persist(new ContributionFileChange(
                    contribution,
                    user,
                    repository,
                    file.path(),
                    file.additions(),
                    file.deletions(),
                    classifier.classify(file.path()),
                    contribution.getOccurredAt()
            ));
        }

        return new CommitDetails(additions, deletions, fetchedFiles.size());
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
                throw providerFailure(response);
            }
            return mapper.readTree(response.body());
        } catch (ProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new ProviderException("GitHub commit detail request failed", 0, e);
        }
    }

    private ProviderException providerFailure(HttpResponse<?> response) {
        int status = response.statusCode();
        OffsetDateTime retryAt = null;

        if (status == 429 || status == 403) {
            retryAt = retryAt(response);
        }

        String remaining = response.headers().firstValue("X-RateLimit-Remaining").orElse(null);
        boolean primaryExhausted = "0".equals(remaining);
        boolean rateLimited = status == 429 || (status == 403 && (primaryExhausted || retryAt != null));
        String message = rateLimited
                ? "GitHub commit detail request rate-limited"
                : "GitHub commit detail request failed";

        return new ProviderException(message, status, retryAt);
    }

    private OffsetDateTime retryAt(HttpResponse<?> response) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        Optional<String> retryAfter = response.headers().firstValue("Retry-After");
        if (retryAfter.isPresent()) {
            try {
                long seconds = Math.max(1, Long.parseLong(retryAfter.get().trim()));
                return now.plusSeconds(seconds);
            } catch (NumberFormatException ignored) {
                // Fall through to the primary-rate-limit reset header.
            }
        }

        Optional<String> reset = response.headers().firstValue("X-RateLimit-Reset");
        if (reset.isPresent()) {
            try {
                long epochSeconds = Long.parseLong(reset.get().trim());
                OffsetDateTime resetAt = OffsetDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneOffset.UTC);
                if (resetAt.isAfter(now)) return resetAt.plusSeconds(1);
            } catch (NumberFormatException ignored) {
                // Fall through to conservative secondary-limit backoff.
            }
        }

        // A 403 secondary limit may omit Retry-After. Stop immediately and wait before the
        // background worker is allowed to issue another provider request.
        return now.plusSeconds(SECONDARY_RATE_LIMIT_FALLBACK_SECONDS);
    }

    record FileInput(String path, int additions, int deletions) {}

    public record CommitDetails(int additions, int deletions, int changedFiles) {}
}
