package io.github.developeranalytics.provider;

import java.time.OffsetDateTime;

public interface SourceControlProvider {
    String providerKey();
    ProviderUser fetchCurrentUser(ProviderAccessToken accessToken) throws ProviderException;
    PagedResult<ProviderRepository> listRepositories(
            ProviderAccessToken accessToken,
            String pageCursor
    ) throws ProviderException;

    ProviderRepositorySnapshot fetchRepositorySnapshot(
            ProviderAccessToken accessToken,
            ProviderRepository repository
    ) throws ProviderException;

    ProviderLanguageBreakdown fetchRepositoryLanguages(
            ProviderAccessToken accessToken,
            ProviderRepository repository
    ) throws ProviderException;

    PagedResult<ProviderContribution> listContributions(
            ProviderAccessToken accessToken,
            ProviderRepository repository,
            OffsetDateTime since,
            String pageCursor
    ) throws ProviderException;
}
