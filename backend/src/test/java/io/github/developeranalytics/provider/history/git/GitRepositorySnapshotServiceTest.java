package io.github.developeranalytics.provider.history.git;

import io.github.developeranalytics.provider.ProviderRepositorySnapshot;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class GitRepositorySnapshotServiceTest {

    @Test
    void readsRelevantFilesAndManifestContentFromBareRepository() throws Exception {
        Path work = Files.createTempDirectory("git-snapshot-work-");
        Path bare = Files.createTempDirectory("git-snapshot-bare-");
        try {
            run(work, "git", "init");
            run(work, "git", "config", "user.email", "test@example.com");
            run(work, "git", "config", "user.name", "Test User");

            Files.createDirectories(work.resolve(".github/workflows"));
            Files.createDirectories(work.resolve("src"));
            Files.writeString(work.resolve("pom.xml"), "<dependency>quarkus-core</dependency>\n");
            Files.writeString(work.resolve("package.json"), "{\"dependencies\":{\"react\":\"1\"}}\n");
            Files.writeString(work.resolve(".github/workflows/ci.yml"), "name: ci\n");
            Files.writeString(work.resolve("src/App.java"), "class App {}\n");

            run(work, "git", "add", ".");
            run(work, "git", "commit", "-m", "snapshot");
            run(work.getParent(), "git", "clone", "--bare", work.toString(), bare.toString());

            GitRepositorySnapshotService service = new GitRepositorySnapshotService();
            ProviderRepositorySnapshot snapshot = service.readSnapshot(bare, null);

            assertEquals(3, snapshot.files().size());
            assertTrue(snapshot.files().stream().anyMatch(file ->
                    file.path().equals("pom.xml") && file.content().contains("quarkus-core")));
            assertTrue(snapshot.files().stream().anyMatch(file ->
                    file.path().equals("package.json") && file.content().contains("react")));
            assertTrue(snapshot.files().stream().anyMatch(file ->
                    file.path().equals(".github/workflows/ci.yml")));
            assertTrue(snapshot.files().stream().noneMatch(file -> file.path().equals("src/App.java")));
        } finally {
            deleteRecursively(work);
            deleteRecursively(bare);
        }
    }

    @Test
    void relevantPathSelectionRemainsBounded() {
        StringBuilder paths = new StringBuilder();
        for (int i = 0; i < 60; i++) {
            paths.append("modules/").append(i).append("/package.json\n");
        }
        assertEquals(GitRepositorySnapshotService.MAX_RELEVANT_FILES,
                GitRepositorySnapshotService.relevantPaths(paths.toString()).size());
    }

    private static void run(Path directory, String... command) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        if (process.waitFor() != 0) {
            throw new IllegalStateException(String.join(" ", command) + " failed: " + output);
        }
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
