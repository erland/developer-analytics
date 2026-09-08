package io.github.developeranalytics.provider.history;

import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;

import java.util.Collection;
import java.util.List;

/**
 * Provider-neutral source of historical per-file commit statistics.
 *
 * Implementations may use a remote API, local Git history, or another source.
 */
public interface ContributionHistoryProvider {

    List<HistoricalCommitFileChanges> fetchFileChanges(
            ProviderAccessToken accessToken,
            ProviderRepository repository,
            Collection<String> commitShas
    ) throws ProviderException;
}
