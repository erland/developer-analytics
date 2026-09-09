package io.github.developeranalytics.provider.snapshot;

import io.github.developeranalytics.observability.StructuredLog;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.ProviderRepositorySnapshot;
import io.github.developeranalytics.provider.git.GitRepositorySnapshotService;
import io.github.developeranalytics.provider.github.GitHubProviderAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/** Resolves repository evidence snapshots using Git first and GitHub REST as a provider fallback. */
@ApplicationScoped
public class RepositorySnapshotProvider {

    private static final Logger LOG = Logger.getLogger(RepositorySnapshotProvider.class);

    @Inject GitRepositorySnapshotService gitSnapshots;
    @Inject GitHubProviderAdapter github;

    public ProviderRepositorySnapshot fetch(
            ProviderAccessToken token,
            ProviderRepository repository
    ) throws ProviderException {
        try {
            ProviderRepositorySnapshot snapshot = gitSnapshots.fetch(token, repository);
            StructuredLog.info(LOG, "repository_snapshot_git",
                    StructuredLog.fields("repository", repository.fullName(), "files", snapshot.files().size()));
            return snapshot;
        } catch (ProviderException gitError) {
            if (Thread.currentThread().isInterrupted()) {
                throw gitError;
            }
            StructuredLog.warn(LOG, "repository_snapshot_git_fallback", gitError,
                    StructuredLog.fields("repository", repository.fullName(), "httpStatus", gitError.getStatusCode()));
            return github.fetchRepositorySnapshot(token, repository);
        }
    }
}
