package io.github.developeranalytics.service.technology;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.domain.technology.*;
import io.github.developeranalytics.persistence.technology.RepositoryTechnologyEvidenceRepository;
import io.github.developeranalytics.persistence.technology.TechnologyCatalogueRepository;
import io.github.developeranalytics.provider.*;
import io.github.developeranalytics.provider.github.GitHubProviderAdapter;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class FileManifestEvidenceService {

    @Inject
    GitHubProviderAdapter github;

    @Inject
    ProviderCredentialService credentials;

    @Inject
    TechnologyCatalogueService catalogueService;

    @Inject
    TechnologyCatalogueRepository catalogue;

    @Inject
    RepositoryTechnologyEvidenceRepository evidenceRepository;

    @Transactional
    public Result collect(AppUser user, SourceRepository repository)
            throws ProviderException {
        catalogueService.seedBuiltInCatalogueIfEmpty();

        ProviderAccessToken token =
                credentials.requireAccessToken(user.getId(), "github");

        ProviderRepository providerRepository = new ProviderRepository(
                repository.getExternalRepositoryId(),
                repository.getOwnerExternalId(),
                repository.getOwnerLogin(),
                repository.getOwnerLogin() == null
                        ? ProviderRepository.OwnerType.OTHER
                        : ProviderRepository.OwnerType.USER,
                repository.getName(),
                repository.getFullName(),
                repository.getHtmlUrl(),
                repository.getVisibility().name().equals("PRIVATE")
                        ? ProviderRepository.Visibility.PRIVATE
                        : ProviderRepository.Visibility.PUBLIC,
                repository.isFork(),
                repository.isArchived(),
                null,
                null,
                repository.getLastActivityAt()
        );

        ProviderRepositorySnapshot snapshot =
                github.fetchRepositorySnapshot(token, providerRepository);

        List<TechnologyCatalogueEntry> technologies = catalogue.findActive();
        OffsetDateTime observedAt = OffsetDateTime.now(ZoneOffset.UTC);

        int fileMatches = 0;
        int manifestMatches = 0;

        for (ProviderRepositoryFile file : snapshot.files()) {
            String normalizedPath = file.path().toLowerCase(Locale.ROOT);
            String content = file.content() == null
                    ? ""
                    : file.content().toLowerCase(Locale.ROOT);

            for (TechnologyCatalogueEntry technology : technologies) {
                for (String pattern : technology.getFileEvidence()) {
                    if (matchesFilePattern(normalizedPath, pattern)) {
                        upsert(
                                user,
                                repository,
                                technology,
                                TechnologyEvidenceType.FILE,
                                file.path(),
                                null,
                                observedAt
                        );
                        fileMatches++;
                    }
                }

                for (String tokenPattern : technology.getManifestEvidence()) {
                    if (!content.isBlank()
                            && content.contains(tokenPattern.toLowerCase(Locale.ROOT))) {
                        upsert(
                                user,
                                repository,
                                technology,
                                TechnologyEvidenceType.MANIFEST,
                                file.path() + ":" + tokenPattern,
                                null,
                                observedAt
                        );
                        manifestMatches++;
                    }
                }
            }
        }

        return new Result(
                snapshot.files().size(),
                fileMatches,
                manifestMatches,
                snapshot.rateLimit()
        );
    }

    boolean matchesFilePattern(String normalizedPath, String rawPattern) {
        String pattern = rawPattern.toLowerCase(Locale.ROOT);

        if (pattern.startsWith("*.")) {
            return normalizedPath.endsWith(pattern.substring(1));
        }

        if (pattern.endsWith("/")) {
            return normalizedPath.startsWith(pattern)
                    || normalizedPath.contains("/" + pattern);
        }

        if (pattern.startsWith(".")) {
            return normalizedPath.equals(pattern)
                    || normalizedPath.endsWith("/" + pattern);
        }

        return normalizedPath.equals(pattern)
                || normalizedPath.endsWith("/" + pattern)
                || normalizedPath.contains("/" + pattern + "/");
    }

    private void upsert(
            AppUser user,
            SourceRepository repository,
            TechnologyCatalogueEntry technology,
            TechnologyEvidenceType type,
            String sourceValue,
            Long measuredValue,
            OffsetDateTime observedAt
    ) {
        RepositoryTechnologyEvidence evidence = evidenceRepository.find(
                repository.getId(),
                technology.getId(),
                type,
                sourceValue
        ).orElse(null);

        if (evidence == null) {
            evidence = new RepositoryTechnologyEvidence(
                    user,
                    repository,
                    technology,
                    type,
                    TechnologyEvidenceStrength.OBSERVED,
                    sourceValue,
                    measuredValue,
                    observedAt
            );
            evidenceRepository.persist(evidence);
        } else {
            evidence.refresh(measuredValue, observedAt);
        }
    }

    public record Result(
            int relevantFilesObserved,
            int fileEvidenceMatches,
            int manifestEvidenceMatches,
            ProviderRateLimit rateLimit
    ) {}
}
