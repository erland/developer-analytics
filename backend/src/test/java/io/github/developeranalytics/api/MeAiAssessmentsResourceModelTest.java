package io.github.developeranalytics.api;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MeAiAssessmentsResourceModelTest {

    @Test
    void writeBackRequestDoesNotAcceptClientOrScopeFromCaller() {
        var request = new MeAiAssessmentsResource.CreateRequest(
                "technology-summary",
                Map.of("summary", "Evolving from Java toward platform tooling"),
                false
        );

        assertEquals("technology-summary", request.analysisType());
        assertFalse(request.containsPrivateData());

        var componentNames = java.util.Arrays.stream(
                MeAiAssessmentsResource.CreateRequest.class
                        .getRecordComponents()
        )
        .map(component -> component.getName())
        .toList();

        assertFalse(componentNames.contains("sourceClient"));
        assertFalse(componentNames.contains("dataScope"));
        assertFalse(componentNames.contains("timestamp"));
    }
}
