package io.github.developeranalytics.service.insight;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserAiInsightVersionTest {

    @Test
    void userInsightAnalysisVersionIsExplicit() {
        assertEquals(
                "user-ai-v1",
                UserAiInsightService.ANALYSIS_VERSION
        );
    }
}
