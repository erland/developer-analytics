package io.github.developeranalytics.provider.history.git;

import io.github.developeranalytics.provider.ProviderAccessToken;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class GitCloneWorkspaceService {

    static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);
    static final long DEFAULT_MAX_TEMPORARY_BYTES = 1_073_741_824L;
    static final long DEFAULT_MIN_FREE_SPACE_BYTES = 536_870_912L;
    private static final Duration RESOURCE_POLL_INTERVAL = Duration.ofMillis(250);
    private static final int MAX_ERROR_CHARS = 4_000;

    @ConfigProperty(name = "developer-analytics.git-history.clone-timeout", defaultValue = "10m")
    Duration cloneTimeout;

    @ConfigProperty(name = "developer-analytics.git-history.max-temporary-bytes", defaultValue = "1073741824")
    long maxTemporaryBytes;

    @ConfigProperty(name = "developer-analytics.git-history.min-free-space-bytes", defaultValue = "536870912")
    long minFreeSpaceBytes;

    /** One historical clone at a time per worker JVM. */
    private final Semaphore clonePermit = new Semaphore(1, true);

    public TemporaryGitRepository cloneBareBlobless(
            URI repositoryUri,
            ProviderAccessToken accessToken
    ) throws IOException, InterruptedException {
        return cloneBareBlobless(
                repositoryUri,
                accessToken,
                cloneTimeout == null ? DEFAULT_TIMEOUT : cloneTimeout,
                positiveOrDefault(maxTemporaryBytes, DEFAULT_MAX_TEMPORARY_BYTES),
                nonNegativeOrDefault(minFreeSpaceBytes, DEFAULT_MIN_FREE_SPACE_BYTES)
        );
    }

    TemporaryGitRepository cloneBareBlobless(
            URI repositoryUri,
            ProviderAccessToken accessToken,
            Duration timeout
    ) throws IOException, InterruptedException {
        return cloneBareBlobless(
                repositoryUri,
                accessToken,
                timeout,
                DEFAULT_MAX_TEMPORARY_BYTES,
                DEFAULT_MIN_FREE_SPACE_BYTES
        );
    }

    TemporaryGitRepository cloneBareBlobless(
            URI repositoryUri,
            ProviderAccessToken accessToken,
            Duration timeout,
            long maxBytes,
            long minFreeBytes
    ) throws IOException, InterruptedException {
        validateInputs(repositoryUri, timeout, maxBytes, minFreeBytes);

        if (!clonePermit.tryAcquire()) {
            throw new IOException("Another historical Git clone is already running on this worker");
        }

        Path parent = null;
        boolean success = false;
        try {
            parent = Files.createTempDirectory("developer-analytics-git-");
            ensureSufficientSpace(parent, maxBytes, minFreeBytes);

            Path target = parent.resolve("repository.git");
            Instant started = Instant.now();
            ProcessBuilder builder = new ProcessBuilder(buildCloneCommand(repositoryUri, target));
            builder.redirectErrorStream(true);
            builder.environment().putAll(authenticationEnvironment(accessToken));
            Process process = builder.start();

            waitForClone(process, parent, timeout, maxBytes, minFreeBytes);

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IOException("Git clone failed with exit code " + process.exitValue() + ": "
                        + sanitize(output, accessToken));
            }

            long sizeBytes = directorySize(parent);
            if (exceedsLimit(sizeBytes, maxBytes)) {
                throw new IOException("Temporary Git data exceeded configured maximum of " + maxBytes + " bytes");
            }

            TemporaryGitRepository result = new TemporaryGitRepository(
                    parent,
                    target,
                    Duration.between(started, Instant.now()),
                    sizeBytes
            );
            success = true;
            return result;
        } finally {
            try {
                if (!success && parent != null) deleteRecursively(parent);
            } finally {
                clonePermit.release();
            }
        }
    }

    private static void validateInputs(URI repositoryUri, Duration timeout, long maxBytes, long minFreeBytes) {
        if (repositoryUri == null || !"https".equalsIgnoreCase(repositoryUri.getScheme())) {
            throw new IllegalArgumentException("An HTTPS repository URI is required");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
        if (maxBytes <= 0) throw new IllegalArgumentException("maxBytes must be positive");
        if (minFreeBytes < 0) throw new IllegalArgumentException("minFreeBytes must not be negative");
    }

    private static void waitForClone(
            Process process,
            Path workspace,
            Duration timeout,
            long maxBytes,
            long minFreeBytes
    ) throws IOException, InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (true) {
            if (process.waitFor(RESOURCE_POLL_INTERVAL.toMillis(), TimeUnit.MILLISECONDS)) return;

            if (System.nanoTime() >= deadline) {
                stop(process);
                throw new IOException("Git clone timed out after " + timeout);
            }

            long sizeBytes = directorySize(workspace);
            if (exceedsLimit(sizeBytes, maxBytes)) {
                stop(process);
                throw new IOException("Temporary Git data exceeded configured maximum of " + maxBytes + " bytes");
            }

            long usableBytes = Files.getFileStore(workspace).getUsableSpace();
            if (usableBytes < minFreeBytes) {
                stop(process);
                throw new IOException("Git clone stopped to preserve configured free-space reserve of "
                        + minFreeBytes + " bytes");
            }
        }
    }

    private static void ensureSufficientSpace(Path workspace, long maxBytes, long minFreeBytes) throws IOException {
        long usableBytes = Files.getFileStore(workspace).getUsableSpace();
        if (!hasSufficientSpace(usableBytes, maxBytes, minFreeBytes)) {
            throw new IOException("Insufficient disk space for historical Git clone: usable=" + usableBytes
                    + " required=" + saturatedAdd(maxBytes, minFreeBytes));
        }
    }

    static boolean hasSufficientSpace(long usableBytes, long maxBytes, long minFreeBytes) {
        return usableBytes >= saturatedAdd(maxBytes, minFreeBytes);
    }

    static boolean exceedsLimit(long sizeBytes, long maxBytes) {
        return sizeBytes > maxBytes;
    }

    private static long saturatedAdd(long left, long right) {
        if (left > Long.MAX_VALUE - right) return Long.MAX_VALUE;
        return left + right;
    }

    private static void stop(Process process) throws InterruptedException {
        process.destroy();
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }

    static List<String> buildCloneCommand(URI repositoryUri, Path target) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("clone");
        command.add("--bare");
        command.add("--filter=blob:none");
        command.add("--no-tags");
        command.add(repositoryUri.toString());
        command.add(target.toString());
        return List.copyOf(command);
    }

    static Map<String, String> authenticationEnvironment(ProviderAccessToken accessToken) {
        if (accessToken == null) return Map.of();
        Map<String, String> environment = new HashMap<>();
        environment.put("GIT_CONFIG_COUNT", "1");
        environment.put("GIT_CONFIG_KEY_0", "http.extraHeader");
        environment.put("GIT_CONFIG_VALUE_0", "Authorization: Bearer " + accessToken.value());
        environment.put("GIT_TERMINAL_PROMPT", "0");
        return Map.copyOf(environment);
    }

    private static long directorySize(Path root) throws IOException {
        if (!Files.exists(root)) return 0L;
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).mapToLong(path -> {
                try { return Files.size(path); }
                catch (IOException e) { return 0L; }
            }).sum();
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (TemporaryGitRepository ignored = new TemporaryGitRepository(root, root, Duration.ZERO, 0)) {
            // AutoCloseable performs recursive cleanup.
        }
    }

    static String sanitize(String output, ProviderAccessToken accessToken) {
        if (output == null || output.isBlank()) return "no output";
        String compact = output.replaceAll("\\s+", " ").strip();
        if (accessToken != null) compact = compact.replace(accessToken.value(), "[REDACTED]");
        return compact.length() <= MAX_ERROR_CHARS ? compact : compact.substring(0, MAX_ERROR_CHARS);
    }

    private static long positiveOrDefault(long value, long defaultValue) {
        return value > 0 ? value : defaultValue;
    }

    private static long nonNegativeOrDefault(long value, long defaultValue) {
        return value >= 0 ? value : defaultValue;
    }
}
