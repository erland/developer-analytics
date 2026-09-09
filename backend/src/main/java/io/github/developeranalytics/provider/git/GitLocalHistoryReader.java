package io.github.developeranalytics.provider.git;

import io.github.developeranalytics.provider.ProviderContributionFileChange;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.history.HistoricalCommitFileChanges;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Reads commit/file statistics from an already cloned bare Git repository. */
@ApplicationScoped
public class GitLocalHistoryReader {

    @Inject GitCommandRunner commands;

    public List<HistoricalCommitFileChanges> read(Path repositoryPath, Collection<String> commitShas)
            throws ProviderException {
        if (repositoryPath == null) throw new IllegalArgumentException("repositoryPath is required");
        if (commitShas == null || commitShas.isEmpty()) return List.of();

        List<HistoricalCommitFileChanges> result = new ArrayList<>();
        for (String commitSha : commitShas) {
            if (commitSha == null || commitSha.isBlank()) continue;
            String sha = commitSha.strip();
            try {
                String parents = commands.run(repositoryPath, null,
                        List.of("rev-list", "--parents", "-n", "1", sha));
                String[] parts = parents.strip().split("\\s+");
                if (parts.length == 0 || !parts[0].equals(sha)) {
                    throw new ProviderException("Git history did not resolve commit " + sha, 0);
                }

                List<String> diffArgs = new ArrayList<>();
                if (parts.length == 1) {
                    diffArgs.addAll(List.of("diff-tree", "--root", "--no-commit-id", "--numstat", "-r", "--no-renames", sha));
                } else {
                    // Merge semantics: compare with the first parent only. This measures what the
                    // merge result adds to the mainline without re-counting side-branch history.
                    diffArgs.addAll(List.of("diff", "--numstat", "--no-renames", parts[1], sha, "--"));
                }

                result.add(new HistoricalCommitFileChanges(sha,
                        parseNumstat(commands.run(repositoryPath, null, diffArgs))));
            } catch (IOException e) {
                throw new ProviderException("Could not read local Git history for commit " + sha, 0, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProviderException("Interrupted while reading local Git history for commit " + sha, 0, e);
            }
        }
        return List.copyOf(result);
    }

    static List<ProviderContributionFileChange> parseNumstat(String output) {
        if (output == null || output.isBlank()) return List.of();
        List<ProviderContributionFileChange> changes = new ArrayList<>();
        for (String line : output.split("\\R")) {
            if (line.isBlank()) continue;
            String[] fields = line.split("\\t", 3);
            if (fields.length != 3 || fields[2].isBlank()) continue;
            int additions = parseCount(fields[0]);
            int deletions = parseCount(fields[1]);
            changes.add(new ProviderContributionFileChange(fields[2], additions, deletions));
        }
        return List.copyOf(changes);
    }

    private static int parseCount(String raw) {
        if (raw == null || raw.isBlank() || "-".equals(raw)) return 0;
        try {
            long value = Long.parseLong(raw);
            return (int) Math.min(Integer.MAX_VALUE, Math.max(0, value));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
