package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.report.CanonicalReport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class CanonicalReportChangeScopeTest {

    @Test
    void legacyConstructorPreservesAllChangeBehaviour() {
        CanonicalReport report = new CanonicalReport(
                CanonicalReport.MODEL_VERSION,
                OffsetDateTime.parse("2026-09-08T00:00:00Z"),
                new CanonicalReport.Summary("Report", "Overview"),
                new CanonicalReport.Period(null, null),
                new CanonicalReport.DataCoverage(1, 1, 0, 1, 2),
                List.of(),
                List.of(),
                new CanonicalReport.Activity(2, Map.of("COMMIT", 1, "ISSUE", 1), List.of()),
                List.of(),
                CanonicalReport.RoleAiAssessment.unavailable(),
                new CanonicalReport.Methodology("Measured", "Inference", "Correction", List.of()),
                CanonicalReport.PrivacyScope.PUBLIC_ONLY
        );

        assertTrue(report.changeScope().allChanges());
        assertTrue(report.changeScope().changeKinds().isEmpty());
        assertTrue(report.methodology().changeScopeStatement().contains("all recorded contribution types"));
    }

    @Test
    void rendererMakesFilteredScopeAndMethodologyExplicit() {
        CanonicalReport report = new CanonicalReport(
                CanonicalReport.MODEL_VERSION,
                OffsetDateTime.parse("2026-09-08T00:00:00Z"),
                new CanonicalReport.Summary("Report", "Filtered overview"),
                new CanonicalReport.Period(null, null),
                new CanonicalReport.DataCoverage(1, 1, 0, 1, 1),
                new CanonicalReport.ChangeScope(false, List.of("CODE", "DOCUMENTATION")),
                List.of(),
                List.of(),
                new CanonicalReport.Activity(1, Map.of("COMMIT", 1), List.of()),
                List.of(),
                CanonicalReport.RoleAiAssessment.unavailable(),
                new CanonicalReport.Methodology(
                        "Measured",
                        "Inference",
                        "Correction",
                        "Filtered commits are based on changed-file classification.",
                        List.of("changed-file classifications")
                ),
                CanonicalReport.PrivacyScope.PUBLIC_ONLY
        );

        String markdown = new MarkdownReportRenderer().render(report, MarkdownReportType.FULL_DEVELOPER_REPORT);

        assertFalse(report.changeScope().allChanges());
        assertEquals(List.of("CODE", "DOCUMENTATION"), report.changeScope().changeKinds());
        assertTrue(markdown.contains("Change scope: `CODE,DOCUMENTATION`"));
        assertTrue(markdown.contains("Filtered commits are based on changed-file classification."));
    }
}
