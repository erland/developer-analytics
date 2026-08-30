package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.report.CanonicalReport;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownReportRendererTest {

    @Test
    void rendersFromCanonicalModelOnly() {
        CanonicalReport report = new CanonicalReport(
                CanonicalReport.MODEL_VERSION,
                OffsetDateTime.parse("2026-08-30T10:00:00Z"),
                new CanonicalReport.Summary(
                        "Developer Analytics report",
                        "Canonical summary"
                ),
                new CanonicalReport.Period(null, null),
                new CanonicalReport.DataCoverage(1, 1, 0, 1, 10),
                List.of(new CanonicalReport.ProjectCategory(
                        "backend-service", "Backend service", 1
                )),
                List.of(),
                new CanonicalReport.Activity(
                        10, Map.of("COMMIT", 10), List.of()
                ),
                List.of(),
                CanonicalReport.RoleAiAssessment.unavailable(),
                new CanonicalReport.Methodology(
                        "Measured.", "Inference.", "Corrections.", List.of()
                ),
                CanonicalReport.PrivacyScope.PUBLIC_ONLY
        );

        String markdown = new MarkdownReportRenderer().render(
                report,
                MarkdownReportType.FULL_DEVELOPER_REPORT
        );

        assertTrue(markdown.contains("Canonical summary"));
        assertTrue(markdown.contains("## Data coverage"));
        assertTrue(markdown.contains("## Project categories"));
        assertTrue(markdown.contains("## Technology analysis"));
        assertTrue(markdown.contains("## Activity"));
        assertTrue(markdown.contains("## Significant projects"));
        assertTrue(markdown.contains("## Role / AI assessment"));
        assertTrue(markdown.contains("## Methodology"));
    }
}
