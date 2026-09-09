package io.github.developeranalytics.provider.git;

import io.github.developeranalytics.observability.StructuredLog;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.history.ContributionHistoryProvider;
import io.github.developeranalytics.provider.history.HistoricalCommitFileChanges;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.List;

/** Historical contribution source backed by a temporary local bare Git clone. */
@ApplicationScoped
public class GitContributionHistoryProvider implements ContributionHistoryProvider {

    private static final Logger LOG = Logger.getLogger(GitContributionHistoryProvider.class);

    @Inject GitWorkspaceService workspaces;
    @Inject GitLocalHistoryReader historyReader;

    @Override
    public List<HistoricalCommitFileChanges> fetchFileChanges(
            ProviderAccessToken accessToken,
            ProviderRepository repository,
            Collection<String> commitShas
    ) throws ProviderException {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        if (commitShas == null || commitShas.isEmpty()) return List.of();

        URI cloneUri = cloneUri(repository);
        try (TemporaryGitRepository workspace = workspaces.cloneBareBlobless(cloneUri, accessToken)) {
            List<HistoricalCommitFileChanges> result = historyReader.read(workspace.repositoryPath(), commitShas);
            StructuredLog.info(
                    LOG,
                    "git_history_backfill_transfer",
                    StructuredLog.fields(
                            "repository", repository.fullName(),
                            "requestedCommits", commitShas.size(),
                            "analyzedCommits", result.size(),
                            "cloneDurationMs", workspace.transferDuration().toMillis(),
                            "temporaryGitBytes", workspace.sizeBytes()
                    )
            );
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderException("Interrupted while preparing local Git history for " + repository.fullName(), 0, e);
        } catch (IOException e) {
            throw new ProviderException("Could not prepare local Git history for " + repository.fullName(), 0, e);
        }
    }

    static URI cloneUri(ProviderRepository repository) {
        String htmlUrl = repository.htmlUrl();
        if (htmlUrl == null || htmlUrl.isBlank()) {
            if (repository.fullName() == null || repository.fullName().isBlank()) {
                throw new IllegalArgumentException("Repository HTML URL or full name is required");
            }
            htmlUrl = "https://github.com/" + repository.fullName();
        }
        String normalized = htmlUrl.endsWith(".git") ? htmlUrl : htmlUrl + ".git";
        URI uri = URI.create(normalized);
        if (!"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalArgumentException("Only HTTPS Git repositories are supported");
        }
        return uri;
    }
}
