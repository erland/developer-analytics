package io.github.developeranalytics.observability;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("privacy")
@Tag("unit")
class SensitiveLoggingContractTest {

    @Test
    void sensitiveFieldNamesAreClassified() {
        for (String field : new String[] {
                "token",
                "accessToken",
                "credential",
                "credentialCiphertext",
                "clientSecret",
                "Authorization",
                "privateSourceContent",
                "prompt",
                "commitDiff"
        }) {
            assertTrue(
                    StructuredLog.isSensitiveKey(field),
                    field
            );
        }

        assertFalse(StructuredLog.isSensitiveKey("repositoryId"));
        assertFalse(StructuredLog.isSensitiveKey("syncId"));
        assertFalse(StructuredLog.isSensitiveKey("backgroundJobId"));
    }
}
