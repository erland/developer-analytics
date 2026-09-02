package io.github.developeranalytics.provider;

import java.time.OffsetDateTime;

public record ProviderContributorStatistics(
        int contributorCount,
        int humanContributorCount,
        int botContributorCount,
        int userCommitCount,
        int repositoryCommitCount,
        long userAdditions,
        long userDeletions,
        OffsetDateTime observedAt
) {}
