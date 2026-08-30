package io.github.developeranalytics.ai;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class DisabledAiProviderTest {

    private final DisabledAiProvider provider =
            new DisabledAiProvider();

    @Test
    void disabledProviderRequiresNoExternalConfiguration() {
        assertFalse(provider.isConfigured());
        assertEquals("disabled", provider.providerId());
        assertEquals("none", provider.modelId());
    }

    @Test
    void everyOperationDegradesToNoResult() {
        assertTrue(provider.classifyProject(
                new AiProvider.ProjectClassificationRequest(
                        "demo",
                        "demo project",
                        List.of("Java"),
                        List.of()
                )
        ).isEmpty());

        assertTrue(provider.summariseProject(
                new AiProvider.ProjectSummaryRequest(
                        "demo",
                        "demo project",
                        List.of("Java"),
                        List.of("commits")
                )
        ).isEmpty());

        assertTrue(provider.normaliseTechnologies(
                new AiProvider.TechnologyNormalisationRequest(
                        List.of("JS", "Postgres")
                )
        ).isEmpty());

        assertTrue(provider.inferRoles(
                new AiProvider.RoleInferenceRequest(
                        List.of("backend-service"),
                        List.of("Java"),
                        List.of("reviews")
                )
        ).isEmpty());

        assertTrue(provider.summariseUserInsights(
                new AiProvider.UserInsightsRequest(
                        List.of(),
                        List.of(),
                        2,
                        0,
                        1,
                        1,
                        10
                )
        ).isEmpty());

        assertTrue(provider.summariseTechnologyHistory(
                new AiProvider.TechnologyHistorySummaryRequest(
                        "Java",
                        List.of(
                                new AiProvider.TechnologyHistoryPoint(
                                        "2026-08",
                                        2,
                                        20
                                )
                        )
                )
        ).isEmpty());
    }
}
