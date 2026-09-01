package io.github.developeranalytics.service.job;

import io.github.developeranalytics.provider.ProviderException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("worker-job")
@Tag("unit")
class JobFailureClassifierTest {

    private final JobFailureClassifier classifier =
            new JobFailureClassifier();

    @Test
    void temporaryGithubErrorsAreRetriable() {
        for (int status : new int[] {408, 429, 500, 502, 503}) {
            var result = classifier.classify(
                    new ProviderException("temporary", status)
            );
            assertTrue(result.retriable(), Integer.toString(status));
            assertFalse(result.providerAccessLost());
        }
    }

    @Test
    void authenticationFailureIsGlobalAccessLoss() {
        var result = classifier.classify(
                new ProviderException("bad credential", 401)
        );
        assertFalse(result.retriable());
        assertTrue(result.providerAccessLost());
    }

    @Test
    void repositoryNotFoundDoesNotRevokeProviderAccess() {
        var result = classifier.classify(
                new ProviderException("repository missing", 404)
        );
        assertFalse(result.retriable());
        assertFalse(result.providerAccessLost());
    }

    @Test
    void forbiddenDoesNotRevokeEveryRepository() {
        var result = classifier.classify(
                new ProviderException("forbidden", 403)
        );
        assertTrue(result.retriable());
        assertFalse(result.providerAccessLost());
    }
}
