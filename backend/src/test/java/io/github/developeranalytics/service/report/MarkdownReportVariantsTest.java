package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.report.CanonicalReport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MarkdownReportVariantsTest {

    private final MarkdownReportRenderer renderer = new MarkdownReportRenderer();

    @Test
    void everyReportIncludesCoverageAndMethodology() {
        CanonicalReport report = report();
        for (MarkdownReportType type : MarkdownReportType.values()) {
            String markdown = renderer.render(report, type);
            assertTrue(markdown.contains("## Data coverage"), type.name());
            assertTrue(markdown.contains("## Methodology"), type.name());
        }
    }

    @Test
    void technologyProfileIsFocused() {
        String markdown = renderer.render(
                report(), MarkdownReportType.TECHNOLOGY_PROFILE
        );
        assertTrue(markdown.contains("## Technology analysis"));
        assertTrue(markdown.contains("## Role / AI assessment"));
        assertFalse(markdown.contains("## Significant projects"));
        assertFalse(markdown.contains("## Activity"));
    }

    @Test
    void activityReportIsFocused() {
        String markdown = renderer.render(
                report(), MarkdownReportType.ACTIVITY_REPORT
        );
        assertTrue(markdown.contains("## Activity"));
        assertTrue(markdown.contains("### Monthly activity"));
        assertFalse(markdown.contains("## Technology analysis"));
        assertFalse(markdown.contains("## Significant projects"));
    }

    private CanonicalReport report() {
        return new CanonicalReport(
                CanonicalReport.MODEL_VERSION,
                OffsetDateTime.parse("2026-08-30T10:00:00Z"),
                new CanonicalReport.Summary("Report", "Summary"),
                new CanonicalReport.Period(null, null),
                new CanonicalReport.DataCoverage(1, 1, 0, 1, 2),
                List.of(new CanonicalReport.ProjectCategory("backend", "Backend", 1)),
                List.of(new CanonicalReport.TechnologyAnalysis(
                        "java", "Java", "STRONG", 80, 1,
                        null, null, "PUBLIC_ONLY"
                )),
                new CanonicalReport.Activity(
                        2,
                        Map.of("COMMIT", 2),
                        List.of(new CanonicalReport.ActivityMonth("2026-08", 2, 1))
                ),
                List.of(),
                CanonicalReport.RoleAiAssessment.unavailable(),
                new CanonicalReport.Methodology(
                        "Measured.", "Inference.", "Corrections.", List.of("repository metadata")
                ),
                CanonicalReport.PrivacyScope.PUBLIC_ONLY
        );
    }
}
