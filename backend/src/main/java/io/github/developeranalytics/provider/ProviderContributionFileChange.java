package io.github.developeranalytics.provider;

public record ProviderContributionFileChange(
        String path,
        int additions,
        int deletions
) {}
