package io.github.developeranalytics.provider.git;

import io.github.developeranalytics.provider.ProviderAccessToken;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class GitCommandRunnerTest {

    @Test
    void runsCommandAgainstBareRepository() throws Exception {
        Path work = Files.createTempDirectory("git-runner-work-");
        Path bare = Files.createTempDirectory("git-runner-bare-");
        try {
            run(work, "git", "init");
            run(work, "git", "config", "user.email", "test@example.invalid");
            run(work, "git", "config", "user.name", "Test User");
            Files.writeString(work.resolve("README.md"), "test\n");
            run(work, "git", "add", ".");
            run(work, "git", "commit", "-m", "initial");
            run(work.getParent(), "git", "clone", "--bare", work.toString(), bare.toString());

            String head = new GitCommandRunner().run(bare, null, List.of("rev-parse", "HEAD"));
            assertFalse(head.isBlank());
        } finally {
            deleteRecursively(work);
            deleteRecursively(bare);
        }
    }

    @Test
    void sanitizesTokenFromFailedCommandOutput() throws Exception {
        Path repository = Files.createTempDirectory("git-runner-failure-");
        ProviderAccessToken token = new ProviderAccessToken("secret-token-value");
        try {
            IOException error = assertThrows(IOException.class, () ->
                    new GitCommandRunner().run(
                            repository,
                            token,
                            List.of("config", "--get", "definitely.missing.key"),
                            Duration.ofSeconds(10)));
            assertFalse(error.getMessage().contains(token.value()));
        } finally {
            deleteRecursively(repository);
        }
    }

    private static void run(Path directory, String... command) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();
        assertEquals(0, exit, () -> String.join(" ", command) + " failed: " + output);
    }

    private static void deleteRecursively(Path root) throws Exception {
        if (root == null || !Files.exists(root)) return;
        try (var paths = Files.walk(root)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
