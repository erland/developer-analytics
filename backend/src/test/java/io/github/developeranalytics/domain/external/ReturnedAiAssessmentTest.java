package io.github.developeranalytics.domain.external;

import io.github.developeranalytics.domain.model.AppUser;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ReturnedAiAssessmentTest {

    @Test
    void storesRequiredAssessmentMetadata() {
        AppUser user = AppUser.create();
        ExternalClientToken token = new ExternalClientToken(
                user,
                "ChatGPT",
                "hash",
                Set.of(ExternalClientToken.Scope.AI_ASSESSMENTS_WRITE),
                ExternalClientToken.PrivacyScope.PUBLIC_ONLY
        );

        ReturnedAiAssessment assessment =
                new ReturnedAiAssessment(
                        user,
                        token,
                        "developer-profile",
                        "ChatGPT",
                        ExternalClientToken.PrivacyScope.PUBLIC_ONLY,
                        Map.of("summary", "Backend-oriented activity"),
                        false
                );

        assertEquals("developer-profile", assessment.getAnalysisType());
        assertEquals("ChatGPT", assessment.getSourceClient());
        assertEquals(
                ExternalClientToken.PrivacyScope.PUBLIC_ONLY,
                assessment.getDataScope()
        );
        assertFalse(assessment.isContainsPrivateData());
        assertEquals(
                "Backend-oriented activity",
                assessment.getContent().get("summary")
        );
        assertNotNull(assessment.getCreatedAt());
    }
}
