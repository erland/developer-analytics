package io.github.developeranalytics.provider.git;

import io.github.developeranalytics.provider.ProviderAccessToken;
import jakarta.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs commands against an already cloned bare Git repository. */
@ApplicationScoped
public class GitCommandRunner {

    static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(2);

    public String run(
            Path repositoryPath,
            ProviderAccessToken accessToken,
            List<String> arguments
    ) throws IOException, InterruptedException {
        return run(repositoryPath, accessToken, arguments, DEFAULT_TIMEOUT);
    }

    String run(
            Path repositoryPath,
            ProviderAccessToken accessToken,
            List<String> arguments,
            Duration timeout
    ) throws IOException, InterruptedException {
        if (repositoryPath == null) throw new IllegalArgumentException("repositoryPath is required");
        if (arguments == null || arguments.isEmpty()) throw new IllegalArgumentException("arguments are required");
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }

        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("--git-dir=" + repositoryPath.toAbsolutePath());
        command.addAll(arguments);

        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true);
        builder.environment().putAll(GitWorkspaceService.authenticationEnvironment(accessToken));
        Process process = builder.start();
        boolean finished;
        try {
            finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException interrupted) {
            stop(process);
            Thread.currentThread().interrupt();
            throw interrupted;
        }

        if (!finished) {
            stop(process);
            throw new IOException("Git command timed out after " + timeout);
        }

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (process.exitValue() != 0) {
            throw new IOException("Git command failed with exit code " + process.exitValue() + ": "
                    + GitWorkspaceService.sanitize(output, accessToken));
        }
        return output;
    }

    private static void stop(Process process) throws InterruptedException {
        process.destroy();
        if (!process.waitFor(5, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
        }
    }
}
