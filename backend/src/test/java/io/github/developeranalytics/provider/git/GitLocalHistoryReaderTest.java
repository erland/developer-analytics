package io.github.developeranalytics.provider.git;

import io.github.developeranalytics.domain.change.ChangeKind;
import io.github.developeranalytics.provider.ProviderContributionFileChange;
import io.github.developeranalytics.provider.history.HistoricalCommitFileChanges;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class GitLocalHistoryReaderTest {

    private final ChangeKindClassifier classifier = new ChangeKindClassifier();

    @Test
    void parsesCodeDocumentationCiAndMixedNumstatWithoutDoubleCounting() {
        String output = String.join("\n",
                "10\t2\tsrc/main/java/App.java",
                "7\t1\tdocs/guide.md",
                "3\t0\t.github/workflows/ci.yml",
                "4\t4\tscripts/release.sh");

        List<ProviderContributionFileChange> changes = GitLocalHistoryReader.parseNumstat(output);

        assertEquals(4, changes.size());
        assertEquals(ChangeKind.CODE, classifier.classify(changes.get(0).path()).kind());
        assertEquals(ChangeKind.DOCUMENTATION, classifier.classify(changes.get(1).path()).kind());
        assertEquals(ChangeKind.CI_CD, classifier.classify(changes.get(2).path()).kind());
        assertEquals(ChangeKind.CODE, classifier.classify(changes.get(3).path()).kind());
        assertEquals(24, changes.stream().mapToInt(ProviderContributionFileChange::additions).sum());
        assertEquals(7, changes.stream().mapToInt(ProviderContributionFileChange::deletions).sum());
    }

    @Test
    void readsExpectedNumstatFromRealLocalGitRepository() throws Exception {
        Path work = Files.createTempDirectory("git-history-acceptance-");
        Path repository = work.resolve("repository");
        Path bare = work.resolve("repository.git");
        try {
            run(work, "git", "init", repository.toString());
            run(repository, "git", "config", "user.email", "test@example.invalid");
            run(repository, "git", "config", "user.name", "Test User");

            Files.createDirectories(repository.resolve("src"));
            Files.writeString(repository.resolve("src/App.java"), "class App {}\n");
            run(repository, "git", "add", ".");
            run(repository, "git", "commit", "-m", "initial code");

            Files.createDirectories(repository.resolve("docs"));
            Files.createDirectories(repository.resolve(".github/workflows"));
            Files.writeString(repository.resolve("src/App.java"), "class App { int value; }\n");
            Files.writeString(repository.resolve("docs/guide.md"), "# Guide\n");
            Files.writeString(repository.resolve(".github/workflows/ci.yml"), "name: CI\n");
            run(repository, "git", "add", ".");
            run(repository, "git", "commit", "-m", "mixed change");
            String sha = run(repository, "git", "rev-parse", "HEAD").strip();

            run(work, "git", "clone", "--bare", repository.toString(), bare.toString());

            GitLocalHistoryReader reader = new GitLocalHistoryReader();
            reader.commands = new GitCommandRunner();
            HistoricalCommitFileChanges result = reader.read(bare, List.of(sha)).get(0);
            assertEquals(sha, result.commitSha());
            assertEquals(3, result.fileChanges().size());
            assertEquals(3, result.fileChanges().stream().map(ProviderContributionFileChange::path).distinct().count());
            assertTrue(result.fileChanges().stream().anyMatch(change -> classifier.classify(change.path()).kind() == ChangeKind.CODE));
            assertTrue(result.fileChanges().stream().anyMatch(change -> classifier.classify(change.path()).kind() == ChangeKind.DOCUMENTATION));
            assertTrue(result.fileChanges().stream().anyMatch(change -> classifier.classify(change.path()).kind() == ChangeKind.CI_CD));
        } finally {
            deleteRecursively(work);
        }
    }

    @Test
    void binaryNumstatIsKeptConservativelyWithZeroCounts() {
        List<ProviderContributionFileChange> changes = GitLocalHistoryReader.parseNumstat("-\t-\tassets/logo.png\n");
        assertEquals(1, changes.size());
        assertEquals("assets/logo.png", changes.get(0).path());
        assertEquals(0, changes.get(0).additions());
        assertEquals(0, changes.get(0).deletions());
        assertEquals(ChangeKind.OTHER, classifier.classify(changes.get(0).path()).kind());
    }

    @Test
    void malformedRowsAreIgnoredInsteadOfInventingFileChanges() {
        List<ProviderContributionFileChange> changes = GitLocalHistoryReader.parseNumstat(
                "not-numstat\n1\t2\t\n2\t3\tvalid.java\n");
        assertEquals(1, changes.size());
        assertEquals("valid.java", changes.get(0).path());
    }

    private static String run(Path directory, String... command) throws Exception {
        Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        String output = new String(process.getInputStream().readAllBytes());
        int exit = process.waitFor();
        assertEquals(0, exit, () -> String.join(" ", command) + " failed: " + output);
        return output;
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
