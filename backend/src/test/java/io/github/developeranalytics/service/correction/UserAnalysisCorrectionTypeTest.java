package io.github.developeranalytics.service.correction;

import io.github.developeranalytics.domain.correction.UserAnalysisCorrection;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

@Tag("unit")
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
