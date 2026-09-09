package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.github.GitHubProviderAdapter;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import io.github.developeranalytics.service.connection.ProviderSession;
import io.github.developeranalytics.service.sync.ProviderRepositoryMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Resolves the stable provider context needed by one GitHub contribution sync. */
@ApplicationScoped
public class GitHubContributionSyncContextResolver {

    @Inject ProviderCredentialService credentials;
    @Inject GitHubProviderAdapter github;
    @Inject ProviderRepositoryMapper repositories;

    public SyncContext resolve(java.util.UUID userId, SourceRepository repository) throws ProviderException {
        ProviderSession providerSession = credentials.requireSession(userId, "github");
        ProviderAccessToken token = providerSession.accessToken();
        String userLogin = providerSession.login();
        if (userLogin == null || userLogin.isBlank()) {
            userLogin = github.fetchCurrentUser(token).login();
        }

        return new SyncContext(token, userLogin, repositories.map(repository));
    }

    public record SyncContext(
            ProviderAccessToken accessToken,
            String userLogin,
            ProviderRepository providerRepository
    ) {}
}
