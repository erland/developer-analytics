package io.github.developeranalytics.provider;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

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
        OffsetDateTime pushedAt,
        String description,
        List<String> topics
) {
    public enum OwnerType { USER, ORGANIZATION, OTHER }
    public enum Visibility { PUBLIC, PRIVATE }

    public ProviderRepository {
        topics = topics == null
                ? List.of()
                : Collections.unmodifiableList(List.copyOf(topics));
    }

    public ProviderRepository(
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
        this(externalRepositoryId, ownerExternalId, ownerLogin, ownerType,
             name, fullName, htmlUrl, visibility, fork, archived,
             createdAt, updatedAt, pushedAt, null, List.of());
    }
}
