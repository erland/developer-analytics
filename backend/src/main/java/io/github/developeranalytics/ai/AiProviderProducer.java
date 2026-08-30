package io.github.developeranalytics.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class AiProviderProducer {

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "developer-analytics.ai.provider")
    String provider;

    @ConfigProperty(name = "developer-analytics.ai.gemini.api-key")
    Optional<String> geminiApiKey;

    @ConfigProperty(name = "developer-analytics.ai.gemini.model")
    String geminiModel;

    @ConfigProperty(name = "developer-analytics.ai.gemini.base-url")
    String geminiBaseUrl;

    @Produces
    @ApplicationScoped
    public AiProvider aiProvider() {
        if (!"gemini".equalsIgnoreCase(provider)) {
            return new DisabledAiProvider();
        }

        String apiKey = geminiApiKey
                .filter(value -> !value.isBlank())
                .orElse(null);

        if (apiKey == null) {
            return new DisabledAiProvider();
        }

        return new GeminiAiProvider(
                apiKey,
                geminiModel,
                geminiBaseUrl,
                objectMapper
        );
    }
}
