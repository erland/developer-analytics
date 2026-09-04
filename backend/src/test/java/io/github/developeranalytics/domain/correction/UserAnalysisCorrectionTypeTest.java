package io.github.developeranalytics.domain.correction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class UserAnalysisCorrectionTypeTest {

    @Test
    void onlyAiProfileExclusionRemainsAsManualAnalysisCorrection() {
        assertArrayEquals(
                new UserAnalysisCorrection.Type[] {
                        UserAnalysisCorrection.Type.PROJECT_EXCLUDED_FROM_AI_PROFILE
                },
                UserAnalysisCorrection.Type.values()
        );
    }
}
