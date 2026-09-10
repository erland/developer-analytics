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

    /**
     * Releases provider-specific resources retained while processing one repository.
     * Implementations that do not retain repository resources may keep the default no-op.
     */
    default void releaseRepository(ProviderRepository repository) {
        // No-op by default.
    }
}
