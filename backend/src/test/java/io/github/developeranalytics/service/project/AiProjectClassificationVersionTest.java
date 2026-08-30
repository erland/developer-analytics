package io.github.developeranalytics.service.project;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class AiProjectClassificationVersionTest {

    @Test
    void analysisVersionIsExplicitAndStable() {
        assertEquals(
                "project-ai-v1",
                AiProjectClassificationService.ANALYSIS_VERSION
        );
    }
}
