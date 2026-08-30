package io.github.developeranalytics.service.correction;

import io.github.developeranalytics.domain.correction.UserAnalysisCorrection;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class UserAnalysisCorrectionTypeTest {

    @Test
    void supportsRequiredCorrectionTypesWithoutChangingSourceFacts() {
        assertArrayEquals(
                new UserAnalysisCorrection.Type[] {
                        UserAnalysisCorrection.Type.PROJECT_CATEGORY_REJECTED,
                        UserAnalysisCorrection.Type.TECHNOLOGY_INFERENCE_SUPPRESSED,
                        UserAnalysisCorrection.Type.PROJECT_EXCLUDED_FROM_AI_PROFILE
                },
                UserAnalysisCorrection.Type.values()
        );
    }
}
