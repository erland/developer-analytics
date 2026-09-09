package io.github.developeranalytics.service.sync;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.RepositoryOwnerType;
import io.github.developeranalytics.domain.model.RepositoryOwnershipRelation;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.provider.ProviderRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class ProviderRepositoryMapperTest {

    @Test
    void preservesRepositoryMetadataAndOwnerType() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(user, "github", "repo-id", "acme", "repo");
        OffsetDateTime activity = OffsetDateTime.now(ZoneOffset.UTC);
        repository.updateFromDiscovery(
                "owner-id",
                "acme",
                "repo",
                "acme/repo",
                "https://github.com/acme/repo",
                "Description",
                List.of("java", "quarkus"),
                RepositoryOwnerType.ORGANIZATION,
                RepositoryOwnershipRelation.ORGANIZATION_OWNED,
                RepositoryVisibility.PRIVATE,
                true,
                false,
                activity,
                activity
        );
        repository.updateRepositorySizeBytes(1234L);

        ProviderRepository mapped = new ProviderRepositoryMapper().map(repository);

        assertEquals(ProviderRepository.OwnerType.ORGANIZATION, mapped.ownerType());
        assertEquals(ProviderRepository.Visibility.PRIVATE, mapped.visibility());
        assertEquals("Description", mapped.description());
        assertEquals(List.of("java", "quarkus"), mapped.topics());
        assertEquals(1234L, mapped.repositorySizeBytes());
        assertEquals(activity, mapped.pushedAt());
    }
}
