package io.github.developeranalytics.service.project;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AiProjectClassificationVersionTest {

    @Test
    void analysisVersionIsExplicitAndStable() {
        assertEquals(
                "project-ai-v1",
                AiProjectClassificationService.ANALYSIS_VERSION
        );
    }
}
