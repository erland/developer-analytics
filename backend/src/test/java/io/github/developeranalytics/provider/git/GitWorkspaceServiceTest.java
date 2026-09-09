package io.github.developeranalytics.provider.git;

import io.github.developeranalytics.provider.ProviderAccessToken;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class GitWorkspaceServiceTest {

    @Test
    void cloneCommandIsBareBloblessAndContainsNoCredential() {
        URI uri = URI.create("https://github.com/example/repository.git");
        List<String> command = GitWorkspaceService.buildCloneCommand(uri, Path.of("/tmp/repository.git"));

        assertEquals("git", command.get(0));
        assertTrue(command.contains("--bare"));
        assertTrue(command.contains("--filter=blob:none"));
        assertTrue(command.contains("--no-tags"));
        assertTrue(command.contains(uri.toString()));
        assertTrue(command.stream().noneMatch(value -> value.contains("secret-token")));
    }

    @Test
    void credentialsArePassedViaEnvironmentInsteadOfCloneUrl() {
        Map<String, String> environment = GitWorkspaceService.authenticationEnvironment(
                new ProviderAccessToken("secret-token"));

        assertEquals("1", environment.get("GIT_CONFIG_COUNT"));
        assertEquals("http.extraHeader", environment.get("GIT_CONFIG_KEY_0"));
        assertEquals("Authorization: Bearer secret-token", environment.get("GIT_CONFIG_VALUE_0"));
        assertEquals("0", environment.get("GIT_TERMINAL_PROMPT"));
    }

    @Test
    void nonAuthenticatedCommandsStillDisableInteractivePrompt() {
        Map<String, String> environment = GitWorkspaceService.authenticationEnvironment(null);
        assertEquals("0", environment.get("GIT_TERMINAL_PROMPT"));
    }

    @Test
    void sanitizesCredentialFromGitOutput() {
        String sanitized = GitWorkspaceService.sanitize(
                "fatal: Authorization: Bearer secret-token failed",
                new ProviderAccessToken("secret-token"));

        assertFalse(sanitized.contains("secret-token"));
        assertTrue(sanitized.contains("[REDACTED]"));
    }

    @Test
    void diskPrecheckRequiresMaximumCloneSizePlusReserve() {
        assertTrue(GitWorkspaceService.hasSufficientSpace(1_500, 1_000, 500));
        assertFalse(GitWorkspaceService.hasSufficientSpace(1_499, 1_000, 500));
    }

    @Test
    void temporaryDataLimitIsStrictlyEnforced() {
        assertFalse(GitWorkspaceService.exceedsLimit(1_000, 1_000));
        assertTrue(GitWorkspaceService.exceedsLimit(1_001, 1_000));
    }

    @Test
    void closingWorkspaceRemovesTemporaryRepositoryRecursively() throws Exception {
        Path root = Files.createTempDirectory("git-workspace-test-");
        Path repository = root.resolve("repository.git");
        Files.createDirectories(repository.resolve("objects/pack"));
        Files.writeString(repository.resolve("objects/pack/test.pack"), "data");

        TemporaryGitRepository workspace = new TemporaryGitRepository(
                root, repository, Duration.ofSeconds(1), 4);
        workspace.close();

        assertFalse(Files.exists(root));
    }

    @Test
    void workspaceExposesRepositoryPathAndTransferMetrics() throws Exception {
        Path root = Files.createTempDirectory("git-workspace-metrics-");
        Path repository = root.resolve("repository.git");
        Files.createDirectories(repository);
        try (TemporaryGitRepository workspace = new TemporaryGitRepository(
                root, repository, Duration.ofSeconds(2), 1234)) {
            assertEquals(Duration.ofSeconds(2), workspace.transferDuration());
            assertEquals(1234, workspace.sizeBytes());
            assertEquals(root, workspace.workspaceRoot());
            assertEquals(repository, workspace.repositoryPath());
        }
    }
}
