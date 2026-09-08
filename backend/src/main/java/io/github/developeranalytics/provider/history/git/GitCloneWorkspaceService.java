package io.github.developeranalytics.provider.history.git;

import io.github.developeranalytics.provider.ProviderAccessToken;
import jakarta.enterprise.context.ApplicationScoped;

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
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class GitCloneWorkspaceService {

    static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(10);
    private static final int MAX_ERROR_CHARS = 4_000;

    public TemporaryGitRepository cloneBareBlobless(
            URI repositoryUri,
            ProviderAccessToken accessToken
    ) throws IOException, InterruptedException {
        return cloneBareBlobless(repositoryUri, accessToken, DEFAULT_TIMEOUT);
    }

    TemporaryGitRepository cloneBareBlobless(
            URI repositoryUri,
            ProviderAccessToken accessToken,
            Duration timeout
    ) throws IOException, InterruptedException {
        if (repositoryUri == null || !"https".equalsIgnoreCase(repositoryUri.getScheme())) {
            throw new IllegalArgumentException("An HTTPS repository URI is required");
        }
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        Path parent = Files.createTempDirectory("developer-analytics-git-");
        Path target = parent.resolve("repository.git");
        Instant started = Instant.now();
        boolean success = false;
        try {
            ProcessBuilder builder = new ProcessBuilder(buildCloneCommand(repositoryUri, target));
            builder.redirectErrorStream(true);
            builder.environment().putAll(authenticationEnvironment(accessToken));
            Process process = builder.start();

            boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!finished) {
                process.destroy();
                if (!process.waitFor(5, TimeUnit.SECONDS)) process.destroyForcibly();
                throw new IOException("Git clone timed out after " + timeout);
            }

            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (process.exitValue() != 0) {
                throw new IOException("Git clone failed with exit code " + process.exitValue() + ": " + sanitize(output));
            }

            long sizeBytes = directorySize(parent);
            TemporaryGitRepository result = new TemporaryGitRepository(
                    parent,
                    Duration.between(started, Instant.now()),
                    sizeBytes
            );
            success = true;
            return result;
        } finally {
            if (!success) deleteRecursively(parent);
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
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).mapToLong(path -> {
                try { return Files.size(path); }
                catch (IOException e) { return 0L; }
            }).sum();
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) return;
        try (TemporaryGitRepository ignored = new TemporaryGitRepository(root, Duration.ZERO, 0)) {
            // AutoCloseable performs recursive cleanup.
        }
    }

    private static String sanitize(String output) {
        if (output == null || output.isBlank()) return "no output";
        String compact = output.replaceAll("\\s+", " ").strip();
        return compact.length() <= MAX_ERROR_CHARS ? compact : compact.substring(0, MAX_ERROR_CHARS);
    }
}
