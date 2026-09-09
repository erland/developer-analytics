package io.github.developeranalytics.provider.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.provider.ProviderContribution;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
