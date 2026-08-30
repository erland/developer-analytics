package io.github.developeranalytics.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class GeminiAiProviderConfigurationTest {

    @Test
    void configuredProviderIsVendorNeutralAiProvider() {
        AiProvider provider =
                new GeminiAiProvider(
                        "test-key",
                        "test-model",
                        "https://example.invalid/v1beta",
                        new ObjectMapper()
                );

        assertTrue(provider.isConfigured());
        assertEquals("gemini", provider.providerId());
        assertEquals("test-model", provider.modelId());
    }
}
