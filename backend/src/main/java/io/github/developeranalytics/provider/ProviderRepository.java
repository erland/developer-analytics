package io.github.developeranalytics.provider;

import java.time.OffsetDateTime;

public record ProviderRepository(
        String externalRepositoryId,
        String ownerExternalId,
        String ownerLogin,
        OwnerType ownerType,
        String name,
        String fullName,
        String htmlUrl,
        Visibility visibility,
        boolean fork,
        boolean archived,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime pushedAt
) {
    public enum OwnerType { USER, ORGANIZATION, OTHER }
    public enum Visibility { PUBLIC, PRIVATE }
}
