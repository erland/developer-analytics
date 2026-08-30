package io.github.developeranalytics.service.project;

import io.github.developeranalytics.domain.project.ProjectSignificanceAssessment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SignificantExternalProjectServiceTest {

    @Test
    void matchReasonsKeepBothDimensionsVisible() {
        assertTrue(
                isHigh(ProjectSignificanceAssessment.Level.HIGH)
        );
        assertTrue(
                isHigh(ProjectSignificanceAssessment.Level.VERY_HIGH)
        );
        assertFalse(
                isHigh(ProjectSignificanceAssessment.Level.MEDIUM)
        );
    }

    private boolean isHigh(ProjectSignificanceAssessment.Level level) {
        return level == ProjectSignificanceAssessment.Level.HIGH
                || level == ProjectSignificanceAssessment.Level.VERY_HIGH;
    }
}
