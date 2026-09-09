package io.github.developeranalytics.worker;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("worker-job")
class GitHubRateLimitJobGateTest {

    @Test
    void gatesOnlyRestHeavyGitHubJobs() {
        assertTrue(GitHubRateLimitJobGate.isRateLimitedJobType(
                GitHubRepositoryDiscoveryJobHandler.JOB_TYPE));
        assertTrue(GitHubRateLimitJobGate.isRateLimitedJobType(
                GitHubContributionDiscoveryJobHandler.JOB_TYPE));
        assertTrue(GitHubRateLimitJobGate.isRateLimitedJobType(
                GitHubLanguageEvidenceJobHandler.JOB_TYPE));

        assertFalse(GitHubRateLimitJobGate.isRateLimitedJobType(
                GitHubChangeKindBackfillJobHandler.JOB_TYPE));
        assertFalse(GitHubRateLimitJobGate.isRateLimitedJobType(
                GitHubFileManifestEvidenceJobHandler.JOB_TYPE));
        assertFalse(GitHubRateLimitJobGate.isRateLimitedJobType("NOOP"));
    }
}
