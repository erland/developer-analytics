package io.github.developeranalytics.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiProviderProducerTest {

    @Test
    void disabledProviderRemainsFallbackWithoutSecret() {
        DisabledAiProvider provider =
                new DisabledAiProvider();

        assertFalse(provider.isConfigured());
        assertEquals(
                "disabled",
                provider.providerId()
        );
    }
}
