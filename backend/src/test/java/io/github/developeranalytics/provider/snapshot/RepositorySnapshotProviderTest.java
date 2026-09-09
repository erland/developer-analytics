package io.github.developeranalytics.provider.snapshot;

import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.ProviderRepositoryFile;
import io.github.developeranalytics.provider.ProviderRepositorySnapshot;
import io.github.developeranalytics.provider.git.GitRepositorySnapshotService;
import io.github.developeranalytics.provider.github.GitHubProviderAdapter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class RepositorySnapshotProviderTest {

    @Test
    void returnsGitSnapshotWithoutRestFallback() throws Exception {
        FakeGit git = new FakeGit(new ProviderRepositorySnapshot(
                List.of(new ProviderRepositoryFile("pom.xml", "git")), null), null);
        FakeGitHub github = new FakeGitHub();
        RepositorySnapshotProvider provider = provider(git, github);

        ProviderRepositorySnapshot snapshot = provider.fetch(new ProviderAccessToken("token"), repository());

        assertEquals("git", snapshot.files().getFirst().content());
        assertEquals(0, github.calls);
    }

    @Test
    void fallsBackToRestOnProviderFailure() throws Exception {
        FakeGit git = new FakeGit(null, new ProviderException("git failed", 0));
        FakeGitHub github = new FakeGitHub();
        RepositorySnapshotProvider provider = provider(git, github);

        ProviderRepositorySnapshot snapshot = provider.fetch(new ProviderAccessToken("token"), repository());

        assertEquals("rest", snapshot.files().getFirst().content());
        assertEquals(1, github.calls);
    }

    @Test
    void interruptedProviderFailureDoesNotFallBackToRest() {
        FakeGit git = new FakeGit(null, new ProviderException("interrupted", 0));
        FakeGitHub github = new FakeGitHub();
        RepositorySnapshotProvider provider = provider(git, github);

        Thread.currentThread().interrupt();
        try {
            assertThrows(ProviderException.class,
                    () -> provider.fetch(new ProviderAccessToken("token"), repository()));
            assertEquals(0, github.calls);
        } finally {
            Thread.interrupted();
        }
    }

    private RepositorySnapshotProvider provider(FakeGit git, FakeGitHub github) {
        RepositorySnapshotProvider provider = new RepositorySnapshotProvider();
        provider.gitSnapshots = git;
        provider.github = github;
        return provider;
    }

    private ProviderRepository repository() {
        return new ProviderRepository(
                "1", "2", "alice", ProviderRepository.OwnerType.USER,
                "repo", "alice/repo", "https://github.com/alice/repo",
                ProviderRepository.Visibility.PRIVATE, false, false,
                null, null, null
        );
    }

    static final class FakeGit extends GitRepositorySnapshotService {
        private final ProviderRepositorySnapshot snapshot;
        private final ProviderException failure;

        FakeGit(ProviderRepositorySnapshot snapshot, ProviderException failure) {
            this.snapshot = snapshot;
            this.failure = failure;
        }

        @Override
        public ProviderRepositorySnapshot fetch(ProviderAccessToken accessToken, ProviderRepository repository)
                throws ProviderException {
            if (failure != null) throw failure;
            return snapshot;
        }
    }

    static final class FakeGitHub extends GitHubProviderAdapter {
        int calls;

        @Override
        public ProviderRepositorySnapshot fetchRepositorySnapshot(
                ProviderAccessToken accessToken,
                ProviderRepository repository
        ) {
            calls++;
            return new ProviderRepositorySnapshot(
                    List.of(new ProviderRepositoryFile("package.json", "rest")), null);
        }
    }
}
