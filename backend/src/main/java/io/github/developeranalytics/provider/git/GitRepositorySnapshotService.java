package io.github.developeranalytics.provider.git;

import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.ProviderRepository;
import io.github.developeranalytics.provider.ProviderRepositoryFile;
import io.github.developeranalytics.provider.ProviderRepositorySnapshot;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reads the bounded technology-evidence snapshot from a temporary bare Git clone. */
@ApplicationScoped
public class GitRepositorySnapshotService {

    static final int MAX_RELEVANT_FILES = 40;
    static final long MAX_FILE_BYTES = 1_048_576L;

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
        List<String> paths = relevantPaths(commands.run(
                repositoryPath,
                accessToken,
                List.of("ls-tree", "-r", "--name-only", "HEAD")
        ));

        List<ProviderRepositoryFile> files = new ArrayList<>();
        for (String path : paths) {
            long size = parseSize(commands.run(
                    repositoryPath,
                    accessToken,
                    List.of("cat-file", "-s", "HEAD:" + path)
            ));
            if (size < 0 || size > MAX_FILE_BYTES) continue;

            String content = commands.run(
                    repositoryPath,
                    accessToken,
                    List.of("show", "HEAD:" + path)
            );
            files.add(new ProviderRepositoryFile(path, content));
        }
        return new ProviderRepositorySnapshot(files, null);
    }

    static List<String> relevantPaths(String output) {
        if (output == null || output.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        for (String raw : output.split("\\R")) {
            String path = raw.strip();
            if (path.isEmpty() || !isRelevantTechnologyFile(path)) continue;
            result.add(path);
            if (result.size() >= MAX_RELEVANT_FILES) break;
        }
        return List.copyOf(result);
    }

    static boolean isRelevantTechnologyFile(String rawPath) {
        String path = rawPath.toLowerCase(Locale.ROOT);
        String name = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
        return name.equals("pom.xml") || name.equals("package.json") || name.equals("dockerfile") ||
                name.equals("docker-compose.yml") || name.equals("docker-compose.yaml") || name.equals("compose.yml") ||
                name.equals("compose.yaml") || name.equals("package.swift") || name.equals("pyproject.toml") ||
                name.equals("requirements.txt") || name.equals(".terraform.lock.hcl") || name.endsWith(".tf") ||
                name.equals("chart.yaml") || name.equals("kustomization.yaml") || name.equals("androidmanifest.xml") ||
                name.equals("project.pbxproj") || name.equals("platformio.ini") || name.endsWith(".ino") ||
                path.startsWith(".github/workflows/") || path.contains("/.github/workflows/") ||
                path.startsWith("db/migration/") || path.contains("/db/migration/");
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
