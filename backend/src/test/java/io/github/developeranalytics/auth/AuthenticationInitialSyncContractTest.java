package io.github.developeranalytics.auth;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class AuthenticationInitialSyncContractTest {
    @Test
    void loginAndPrivateAuthorisationQueueRepositoryDiscovery() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/io/github/developeranalytics/auth/AuthenticationService.java"));

        assertTrue(source.contains("discoveryJobs.enqueueGitHubDiscovery(identity.getUser())"));
        assertTrue(source.contains("discoveryJobs.enqueueGitHubDiscovery(current.getUser())"));
    }
}
