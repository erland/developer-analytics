package io.github.developeranalytics.service.sync;

import io.github.developeranalytics.domain.model.RepositoryOwnerType;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.provider.ProviderRepository;
import jakarta.enterprise.context.ApplicationScoped;

/** Maps persisted repository metadata to the provider-neutral repository model. */
@ApplicationScoped
public class ProviderRepositoryMapper {

    public ProviderRepository map(SourceRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository is required");

        ProviderRepository.OwnerType ownerType = switch (repository.getOwnerType()) {
            case USER -> ProviderRepository.OwnerType.USER;
            case ORGANIZATION -> ProviderRepository.OwnerType.ORGANIZATION;
            default -> ProviderRepository.OwnerType.OTHER;
        };

        ProviderRepository.Visibility visibility = repository.getVisibility() == RepositoryVisibility.PRIVATE
                ? ProviderRepository.Visibility.PRIVATE
                : ProviderRepository.Visibility.PUBLIC;

        return new ProviderRepository(
                repository.getExternalRepositoryId(),
                repository.getOwnerExternalId(),
                repository.getOwnerLogin(),
                ownerType,
                repository.getName(),
                repository.getFullName(),
                repository.getHtmlUrl(),
                visibility,
                repository.isFork(),
                repository.isArchived(),
                null,
                null,
                repository.getLastActivityAt()
        );
    }
}
