package io.github.developeranalytics.provider.git;

import io.github.developeranalytics.observability.StructuredLog;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.history.ContributionHistoryProvider;
import io.github.developeranalytics.provider.history.HistoricalCommitFileChanges;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Historical contribution source backed by a reusable local bare Git clone. */
@ApplicationScoped
public class GitContributionHistoryProvider implements ContributionHistoryProvider {

    private static final Logger LOG = Logger.getLogger(GitContributionHistoryProvider.class);
    private static final Duration MAX_IDLE = Duration.ofHours(6);
    static final int MAX_CACHED_WORKSPACES = 2;

    @Inject GitWorkspaceService workspaces;
    @Inject GitLocalHistoryReader historyReader;

    private final Map<URI, CachedWorkspace> cachedWorkspaces = new ConcurrentHashMap<>();

    @Override
    public List<HistoricalCommitFileChanges> fetchFileChanges(
            ProviderAccessToken accessToken,
            ProviderRepository repository,
            Collection<String> commitShas
    ) throws ProviderException {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        if (commitShas == null || commitShas.isEmpty()) return List.of();

        URI cloneUri = cloneUri(repository);
        cleanupExpired();
        CachedWorkspace cached = cachedWorkspace(cloneUri);

        synchronized (cached) {
            boolean reused = cached.workspace != null && Files.isDirectory(cached.workspace.repositoryPath());
            if (!reused) {
                closeQuietly(cached.workspace);
                try {
                    cached.workspace = workspaces.cloneBareBlobless(cloneUri, accessToken);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    cachedWorkspaces.remove(cloneUri, cached);
                    throw new ProviderException("Interrupted while preparing local Git history for " + repository.fullName(), 0, e);
                } catch (IOException e) {
                    cachedWorkspaces.remove(cloneUri, cached);
                    throw new ProviderException("Could not prepare local Git history for " + repository.fullName(), 0, e);
                }
            }

            cached.lastUsed = Instant.now();
            try {
                List<HistoricalCommitFileChanges> result = historyReader.read(cached.workspace.repositoryPath(), commitShas);
                cached.lastUsed = Instant.now();
                StructuredLog.info(
                        LOG,
                        "git_history_backfill_transfer",
                        StructuredLog.fields(
                                "repository", repository.fullName(),
                                "requestedCommits", commitShas.size(),
                                "analyzedCommits", result.size(),
                                "workspaceReused", reused,
                                "cloneDurationMs", reused ? 0 : cached.workspace.transferDuration().toMillis(),
                                "temporaryGitBytes", cached.workspace.sizeBytes(),
                                "cachedWorkspaces", cachedWorkspaces.size()
                        )
                );
                return result;
            } catch (ProviderException e) {
                if (e.getCause() instanceof IOException) {
                    cachedWorkspaces.remove(cloneUri, cached);
                    closeQuietly(cached.workspace);
                    cached.workspace = null;
                }
                throw e;
            }
        }
    }

    private CachedWorkspace cachedWorkspace(URI cloneUri) {
        synchronized (cachedWorkspaces) {
            CachedWorkspace existing = cachedWorkspaces.get(cloneUri);
            if (existing != null) return existing;

            while (cachedWorkspaces.size() >= MAX_CACHED_WORKSPACES) {
                Map.Entry<URI, CachedWorkspace> oldest = cachedWorkspaces.entrySet().stream()
                        .min(Map.Entry.comparingByValue((left, right) -> left.lastUsed.compareTo(right.lastUsed)))
                        .orElse(null);
                if (oldest == null) break;
                evict(oldest.getKey(), oldest.getValue());
            }

            CachedWorkspace created = new CachedWorkspace();
            cachedWorkspaces.put(cloneUri, created);
            return created;
        }
    }

    @Override
    public void releaseRepository(ProviderRepository repository) {
        if (repository == null) return;
        URI cloneUri;
        try {
            cloneUri = cloneUri(repository);
        } catch (RuntimeException ignored) {
            return;
        }
        CachedWorkspace cached = cachedWorkspaces.remove(cloneUri);
        if (cached == null) return;
        synchronized (cached) {
            closeQuietly(cached.workspace);
            cached.workspace = null;
        }
    }

    @PreDestroy
    void closeCachedWorkspaces() {
        for (Map.Entry<URI, CachedWorkspace> entry : cachedWorkspaces.entrySet()) {
            evict(entry.getKey(), entry.getValue());
        }
    }

    private void cleanupExpired() {
        Instant cutoff = Instant.now().minus(MAX_IDLE);
        for (Map.Entry<URI, CachedWorkspace> entry : cachedWorkspaces.entrySet()) {
            CachedWorkspace cached = entry.getValue();
            if (!cached.lastUsed.isAfter(cutoff)) {
                evict(entry.getKey(), cached);
            }
        }
    }

    private void evict(URI cloneUri, CachedWorkspace cached) {
        if (!cachedWorkspaces.remove(cloneUri, cached)) return;
        synchronized (cached) {
            closeQuietly(cached.workspace);
            cached.workspace = null;
        }
    }

    private static void closeQuietly(TemporaryGitRepository workspace) {
        if (workspace == null) return;
        try {
            workspace.close();
        } catch (IOException e) {
            LOG.warnf(e, "Could not remove cached temporary Git workspace %s", workspace.workspaceRoot());
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

    private static final class CachedWorkspace {
        private TemporaryGitRepository workspace;
        private Instant lastUsed = Instant.now();
    }
}
