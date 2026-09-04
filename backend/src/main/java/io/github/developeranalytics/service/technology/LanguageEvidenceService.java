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
public class LanguageEvidenceService {

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

        ProviderLanguageBreakdown languages =
                github.fetchRepositoryLanguages(token, providerRepository);

        List<TechnologyCatalogueEntry> technologies = catalogue.findActive();
        OffsetDateTime observedAt = OffsetDateTime.now(ZoneOffset.UTC);

        int matched = 0;
        long totalBytes = 0;

        for (var language : languages.bytesByLanguage().entrySet()) {
            totalBytes += language.getValue();

            TechnologyCatalogueEntry technology =
                    findLanguageTechnology(technologies, language.getKey());

            if (technology == null) {
                technology = catalogueService.ensureLanguageTechnology(language.getKey());
            }

            RepositoryTechnologyEvidence evidence =
                    evidenceRepository.find(
                            repository.getId(),
                            technology.getId(),
                            TechnologyEvidenceType.LANGUAGE,
                            language.getKey()
                    ).orElse(null);

            if (evidence == null) {
                evidence = new RepositoryTechnologyEvidence(
                        user,
                        repository,
                        technology,
                        TechnologyEvidenceType.LANGUAGE,
                        TechnologyEvidenceStrength.OBSERVED,
                        language.getKey(),
                        language.getValue(),
                        observedAt
                );
                evidenceRepository.persist(evidence);
            } else {
                evidence.refresh(language.getValue(), observedAt);
            }

            matched++;
        }

        return new Result(
                languages.bytesByLanguage().size(),
                matched,
                totalBytes,
                languages.rateLimit()
        );
    }

    private TechnologyCatalogueEntry findLanguageTechnology(
            List<TechnologyCatalogueEntry> technologies,
            String providerLanguage
    ) {
        String target = providerLanguage.toLowerCase(Locale.ROOT);

        for (TechnologyCatalogueEntry technology : technologies) {
            for (String evidence : technology.getLanguageEvidence()) {
                if (evidence.toLowerCase(Locale.ROOT).equals(target)) {
                    return technology;
                }
            }
        }
        return null;
    }

    public record Result(
            int providerLanguagesObserved,
            int catalogueMatches,
            long totalLanguageBytes,
            ProviderRateLimit rateLimit
    ) {}
}
