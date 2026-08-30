package io.github.developeranalytics.observability;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("privacy")
@Tag("unit")
class StructuredLogRedactionTest {

    @Test
    void sensitiveKeysAreNeverFormatted() {
        String formatted = StructuredLog.format(
                "test_event",
                Map.of(
                        "jobId", "123",
                        "token", "secret-token-value",
                        "credentialCiphertext", "encrypted-secret",
                        "privateSourceContent", "class Secret {}",
                        "prompt", "private prompt"
                )
        );

        assertTrue(formatted.contains("jobId=123"));
        assertFalse(formatted.contains("secret-token-value"));
        assertFalse(formatted.contains("encrypted-secret"));
        assertFalse(formatted.contains("class_Secret"));
        assertFalse(formatted.contains("private_prompt"));
    }

    @Test
    void errorMessagesRedactCommonCredentialQueryValues() {
        String sanitized = StructuredLog.sanitize(
                "request failed access_token=abc123 api_key=xyz789"
        );
        assertFalse(sanitized.contains("abc123"));
        assertFalse(sanitized.contains("xyz789"));
        assertTrue(sanitized.contains("[REDACTED]"));
    }
}
