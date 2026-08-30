package io.github.developeranalytics.observability;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
