package io.github.developeranalytics.domain.change;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("unit")
class ChangeKindClassificationTest {

    @Test
    void preservesExplainableClassificationMetadata() {
        ChangeKindClassification classification = new ChangeKindClassification(
                ChangeKind.DOCUMENTATION,
                1.0,
                "markdown-extension",
                "1"
        );

        assertEquals(ChangeKind.DOCUMENTATION, classification.kind());
        assertEquals(1.0, classification.confidence());
        assertEquals("markdown-extension", classification.ruleKey());
        assertEquals("1", classification.classifierVersion());
    }

    @Test
    void fallsBackSafelyToOtherForUnclassifiedFiles() {
        ChangeKindClassification classification = ChangeKindClassification.otherFallback("1");

        assertEquals(ChangeKind.OTHER, classification.kind());
        assertEquals(0.0, classification.confidence());
        assertEquals(ChangeKindClassification.FALLBACK_RULE_KEY, classification.ruleKey());
        assertEquals("1", classification.classifierVersion());
    }

    @Test
    void rejectsConfidenceOutsideSupportedRange() {
        assertThrows(IllegalArgumentException.class, () ->
                new ChangeKindClassification(ChangeKind.CODE, -0.1, "source-extension", "1"));
        assertThrows(IllegalArgumentException.class, () ->
                new ChangeKindClassification(ChangeKind.CODE, 1.1, "source-extension", "1"));
        assertThrows(IllegalArgumentException.class, () ->
                new ChangeKindClassification(ChangeKind.CODE, Double.NaN, "source-extension", "1"));
    }

    @Test
    void rejectsMissingClassificationMetadata() {
        assertThrows(NullPointerException.class, () ->
                new ChangeKindClassification(null, 1.0, "source-extension", "1"));
        assertThrows(IllegalArgumentException.class, () ->
                new ChangeKindClassification(ChangeKind.CODE, 1.0, " ", "1"));
        assertThrows(IllegalArgumentException.class, () ->
                new ChangeKindClassification(ChangeKind.CODE, 1.0, "source-extension", " "));
    }
}
