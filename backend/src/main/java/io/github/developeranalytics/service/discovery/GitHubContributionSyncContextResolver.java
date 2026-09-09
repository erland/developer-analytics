package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.github.GitHubProviderAdapter;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import io.github.developeranalytics.service.connection.ProviderSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Resolves the stable provider context needed by one GitHub contribution sync. */
@ApplicationScoped
public class GitHubContributionSyncContextResolver {

    @Inject ProviderCredentialService credentials;
    @Inject GitHubProviderAdapter github;

    public SyncContext resolve(java.util.UUID userId, SourceRepository repository) throws ProviderException {
        ProviderSession providerSession = credentials.requireSession(userId, "github");
        ProviderAccessToken token = providerSession.accessToken();
        String userLogin = providerSession.login();
        if (userLogin == null || userLogin.isBlank()) {
            userLogin = github.fetchCurrentUser(token).login();
        }

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
                repository.getVisibility() == RepositoryVisibility.PRIVATE
                        ? ProviderRepository.Visibility.PRIVATE
                        : ProviderRepository.Visibility.PUBLIC,
                repository.isFork(),
                repository.isArchived(),
                null,
                null,
                repository.getLastActivityAt()
        );

        return new SyncContext(token, userLogin, providerRepository);
    }

    public record SyncContext(
            ProviderAccessToken accessToken,
            String userLogin,
            ProviderRepository providerRepository
    ) {}
}
