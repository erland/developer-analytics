package io.github.developeranalytics.provider.history.git;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Objects;

/**
 * A short-lived bare Git repository used for historical analysis.
 * Closing the workspace recursively removes all temporary data.
 */
public final class TemporaryGitRepository implements AutoCloseable {

    private final Path workspaceRoot;
    private final Path repositoryPath;
    private final Duration transferDuration;
    private final long sizeBytes;

    public TemporaryGitRepository(
            Path workspaceRoot,
            Path repositoryPath,
            Duration transferDuration,
            long sizeBytes
    ) {
        this.workspaceRoot = Objects.requireNonNull(workspaceRoot, "workspaceRoot");
        this.repositoryPath = Objects.requireNonNull(repositoryPath, "repositoryPath");
        this.transferDuration = Objects.requireNonNull(transferDuration, "transferDuration");
        if (!repositoryPath.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("repositoryPath must be inside workspaceRoot");
        }
        if (sizeBytes < 0) throw new IllegalArgumentException("sizeBytes must be non-negative");
        this.sizeBytes = sizeBytes;
    }

    public Path workspaceRoot() { return workspaceRoot; }
    public Path repositoryPath() { return repositoryPath; }
    public Duration transferDuration() { return transferDuration; }
    public long sizeBytes() { return sizeBytes; }

    @Override
    public void close() throws IOException {
        if (!Files.exists(workspaceRoot)) return;
        try (var paths = Files.walk(workspaceRoot)) {
            for (Path candidate : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(candidate);
            }
        }
    }
}
