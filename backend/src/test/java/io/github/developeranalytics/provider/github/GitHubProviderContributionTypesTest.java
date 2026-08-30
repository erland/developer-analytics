package io.github.developeranalytics.provider.github;

import io.github.developeranalytics.provider.ProviderContribution;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("github-adapter")
class GitHubProviderContributionTypesTest {

    @Test
    void providerModelSupportsReviewsAndIssues() {
        assertEquals(
                ProviderContribution.Type.REVIEW,
                ProviderContribution.Type.valueOf("REVIEW")
        );
        assertEquals(
                ProviderContribution.Type.ISSUE,
                ProviderContribution.Type.valueOf("ISSUE")
        );
    }
}
