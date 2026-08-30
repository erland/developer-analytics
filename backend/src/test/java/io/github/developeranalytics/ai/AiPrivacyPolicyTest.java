package io.github.developeranalytics.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiPrivacyPolicyTest {

    @Test
    void privateMetadataRequiresExplicitAllowedPolicy() {
        assertTrue(
                AiPrivacyPolicy.PRIVATE_METADATA_ALLOWED
                        .allowsPrivateMetadata()
        );
        assertFalse(
                AiPrivacyPolicy.PUBLIC_ONLY
                        .allowsPrivateMetadata()
        );
        assertFalse(
                AiPrivacyPolicy.PRIVATE_AI_DISABLED
                        .allowsPrivateMetadata()
        );
    }

    @Test
    void requestContextRequiresExplicitSensitivity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AiRequestContext(
                        java.util.UUID.randomUUID(),
                        null
                )
        );
    }
}
