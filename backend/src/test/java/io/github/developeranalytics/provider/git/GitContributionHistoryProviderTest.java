package io.github.developeranalytics.provider.git;

import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.history.HistoricalCommitFileChanges;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class GitContributionHistoryProviderTest {

    @Test
    void reusesWorkspaceUntilRepositoryIsReleased() throws Exception {
        FakeWorkspaceService workspaces = new FakeWorkspaceService();
        GitContributionHistoryProvider provider = provider(workspaces);
        ProviderRepository repository = repository("owner/repo");

        provider.fetchFileChanges(null, repository, List.of("abc"));
        Path firstWorkspace = workspaces.lastWorkspace;
        provider.fetchFileChanges(null, repository, List.of("def"));

        assertEquals(1, workspaces.cloneCount);
        assertTrue(Files.exists(firstWorkspace));

        provider.releaseRepository(repository);

        assertFalse(Files.exists(firstWorkspace));

        provider.fetchFileChanges(null, repository, List.of("ghi"));
        assertEquals(2, workspaces.cloneCount);
        provider.releaseRepository(repository);
    }

    @Test
    void evictsOldestWorkspaceWhenCacheLimitIsReached() throws Exception {
        FakeWorkspaceService workspaces = new FakeWorkspaceService();
        GitContributionHistoryProvider provider = provider(workspaces);

        for (int index = 0; index < GitContributionHistoryProvider.MAX_CACHED_WORKSPACES; index++) {
            provider.fetchFileChanges(null, repository("owner/repo-" + index), List.of("sha-" + index));
        }
        Path oldestWorkspace = workspaces.createdWorkspaces.get(0);
        assertTrue(Files.exists(oldestWorkspace));

        provider.fetchFileChanges(null, repository("owner/repo-extra"), List.of("sha-extra"));

        assertEquals(GitContributionHistoryProvider.MAX_CACHED_WORKSPACES + 1, workspaces.cloneCount);
        assertFalse(Files.exists(oldestWorkspace));
        provider.closeCachedWorkspaces();
    }

    private static GitContributionHistoryProvider provider(FakeWorkspaceService workspaces) {
        GitContributionHistoryProvider provider = new GitContributionHistoryProvider();
        provider.workspaces = workspaces;
        provider.historyReader = new EmptyHistoryReader();
        return provider;
    }

    private static ProviderRepository repository(String fullName) {
        return new ProviderRepository(
                "1", "2", "owner", ProviderRepository.OwnerType.USER,
                "repo", fullName, "https://github.com/" + fullName,
                ProviderRepository.Visibility.PRIVATE, false, false,
                null, null, null
        );
    }

    private static final class FakeWorkspaceService extends GitWorkspaceService {
        private int cloneCount;
        private Path lastWorkspace;
        private final List<Path> createdWorkspaces = new ArrayList<>();

        @Override
        public TemporaryGitRepository cloneBareBlobless(URI repositoryUri, ProviderAccessToken accessToken)
                throws IOException {
            cloneCount++;
            lastWorkspace = Files.createTempDirectory("git-history-provider-test-");
            createdWorkspaces.add(lastWorkspace);
            Path repositoryPath = Files.createDirectories(lastWorkspace.resolve("repository.git"));
            return new TemporaryGitRepository(lastWorkspace, repositoryPath, Duration.ofMillis(1), 0);
        }
    }

    private static final class EmptyHistoryReader extends GitLocalHistoryReader {
        @Override
        public List<HistoricalCommitFileChanges> read(Path repositoryPath, Collection<String> commitShas) {
            return List.of();
        }
    }
}
