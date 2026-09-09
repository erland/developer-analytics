package io.github.developeranalytics.provider.git;

import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.ProviderRepositoryFile;
import io.github.developeranalytics.provider.ProviderRepositorySnapshot;
import io.github.developeranalytics.provider.RepositorySnapshotFilePolicy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Reads the bounded technology-evidence snapshot from a temporary bare Git clone. */
@ApplicationScoped
public class GitRepositorySnapshotService {

    @Inject GitWorkspaceService workspaces;
    @Inject GitCommandRunner commands;

    public ProviderRepositorySnapshot fetch(
            ProviderAccessToken accessToken,
            ProviderRepository repository
    ) throws ProviderException {
        if (repository == null) throw new IllegalArgumentException("repository is required");

        try (TemporaryGitRepository workspace = workspaces.cloneBareBlobless(
                GitContributionHistoryProvider.cloneUri(repository), accessToken)) {
            return readSnapshot(workspace.repositoryPath(), accessToken);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ProviderException("Interrupted while reading repository snapshot from Git for "
                    + repository.fullName(), 0, e);
        } catch (IOException e) {
            throw new ProviderException("Could not read repository snapshot from Git for "
                    + repository.fullName(), 0, e);
        }
    }

    ProviderRepositorySnapshot readSnapshot(
            Path repositoryPath,
            ProviderAccessToken accessToken
    ) throws IOException, InterruptedException {
        List<String> paths = RepositorySnapshotFilePolicy.selectRelevantPaths(Arrays.asList(commands.run(
                repositoryPath,
                accessToken,
                List.of("ls-tree", "-r", "--name-only", "HEAD")
        ).split("\\R")));

        List<ProviderRepositoryFile> files = new ArrayList<>();
        for (String path : paths) {
            long size = parseSize(commands.run(
                    repositoryPath,
                    accessToken,
                    List.of("cat-file", "-s", "HEAD:" + path)
            ));
            if (!RepositorySnapshotFilePolicy.isReadableFileSize(size)) continue;

            String content = commands.run(
                    repositoryPath,
                    accessToken,
                    List.of("show", "HEAD:" + path)
            );
            files.add(new ProviderRepositoryFile(path, content));
        }
        return new ProviderRepositorySnapshot(files, null);
    }

    private static long parseSize(String output) {
        if (output == null) return -1L;
        try {
            return Long.parseLong(output.strip());
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }
}
