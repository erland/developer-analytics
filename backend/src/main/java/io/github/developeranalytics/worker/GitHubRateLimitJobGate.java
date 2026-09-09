package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.github.GitHubRateLimitService;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class GitHubRateLimitJobGate {

    private static final Set<String> RATE_LIMITED_JOB_TYPES = Set.of(
            GitHubRepositoryDiscoveryJobHandler.JOB_TYPE,
            GitHubContributionDiscoveryJobHandler.JOB_TYPE,
            GitHubLanguageEvidenceJobHandler.JOB_TYPE
    );

    @Inject ProviderCredentialService credentials;
    @Inject GitHubRateLimitService rateLimits;

    public Optional<GitHubRateLimitService.GitHubRateLimitDecision> blockingDecision(BackgroundJob job) {
        if (job == null || !isRateLimitedJobType(job.getJobType()) || job.getUser() == null) {
            return Optional.empty();
        }

        ProviderAccessToken token = credentials.requireAccessToken(job.getUser().getId(), "github");
        GitHubRateLimitService.GitHubRateLimitDecision decision = rateLimits.decision(token);
        return decision.allowed() ? Optional.empty() : Optional.of(decision);
    }

    static boolean isRateLimitedJobType(String jobType) {
        return jobType != null && RATE_LIMITED_JOB_TYPES.contains(jobType);
    }
}
