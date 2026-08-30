package io.github.developeranalytics.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class SignificantExternalProjectApiModelTest {

    @Test
    void responseKeepsSignificanceAndInvolvementEvidenceSeparate() {
        var entry =
                new MeSignificantExternalProjectsResource.Entry(
                        UUID.randomUUID(),
                        "external-project",
                        "https://github.com/example/external-project",
                        "ORGANIZATION_OWNED",
                        "BOTH",
                        "VERY_HIGH",
                        88,
                        Map.of("ecosystemScore", 20),
                        "HIGH",
                        67,
                        Map.of("relativeContribution", 0.31),
                        OffsetDateTime.parse("2026-08-30T08:00:00Z")
                );

        assertEquals("BOTH", entry.matchReason());
        assertTrue(entry.significanceEvidence().containsKey("ecosystemScore"));
        assertTrue(entry.involvementEvidence().containsKey("relativeContribution"));
    }
}
