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
    void lostRepositoryPermissionIsTerminal() {
        for (int status : new int[] {401, 403, 404}) {
            var result = classifier.classify(
                    new ProviderException("lost access", status)
            );
            assertFalse(result.retriable());
            assertTrue(result.providerAccessLost());
        }
    }
}
