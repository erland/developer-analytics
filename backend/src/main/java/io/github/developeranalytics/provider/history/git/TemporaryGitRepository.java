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

    private final Path path;
    private final Duration transferDuration;
    private final long sizeBytes;

    public TemporaryGitRepository(Path path, Duration transferDuration, long sizeBytes) {
        this.path = Objects.requireNonNull(path, "path");
        this.transferDuration = Objects.requireNonNull(transferDuration, "transferDuration");
        if (sizeBytes < 0) throw new IllegalArgumentException("sizeBytes must be non-negative");
        this.sizeBytes = sizeBytes;
    }

    public Path path() { return path; }
    public Duration transferDuration() { return transferDuration; }
    public long sizeBytes() { return sizeBytes; }

    @Override
    public void close() throws IOException {
        if (!Files.exists(path)) return;
        try (var paths = Files.walk(path)) {
            for (Path candidate : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(candidate);
            }
        }
    }
}
