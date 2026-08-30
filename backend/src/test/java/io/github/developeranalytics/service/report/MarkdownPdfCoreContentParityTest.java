package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.report.CanonicalReport;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class MarkdownPdfCoreContentParityTest {

    @Test
    void markdownAndPdfContainMatchingCoreReportContent() throws Exception {
        CanonicalReport report = new CanonicalReport(
                CanonicalReport.MODEL_VERSION,
                OffsetDateTime.parse("2026-08-30T10:00:00Z"),
                new CanonicalReport.Summary(
                        "Report",
                        "Canonical parity summary"
                ),
                new CanonicalReport.Period(
                        OffsetDateTime.parse("2025-01-01T00:00:00Z"),
                        OffsetDateTime.parse("2026-08-30T00:00:00Z")
                ),
                new CanonicalReport.DataCoverage(2, 1, 1, 2, 42),
                List.of(new CanonicalReport.ProjectCategory(
                        "backend-service",
                        "Backend service",
                        2
                )),
                List.of(new CanonicalReport.TechnologyAnalysis(
                        "java",
                        "Java",
                        "STRONG",
                        88,
                        2,
                        OffsetDateTime.parse("2025-01-01T00:00:00Z"),
                        OffsetDateTime.parse("2026-08-30T00:00:00Z"),
                        "INCLUDES_PRIVATE"
                )),
                new CanonicalReport.Activity(
                        42,
                        Map.of("COMMIT", 40, "PULL_REQUEST", 2),
                        List.of(new CanonicalReport.ActivityMonth(
                                "2026-08",
                                12,
                                2
                        ))
                ),
                List.of(new CanonicalReport.SignificantProject(
                        java.util.UUID.randomUUID(),
                        "Representative project",
                        "PUBLIC",
                        "OWNED_BY_USER",
                        "HIGH",
                        81,
                        "HIGH",
                        76
                )),
                CanonicalReport.RoleAiAssessment.unavailable(),
                new CanonicalReport.Methodology(
                        "Measured evidence statement.",
                        "Inference statement.",
                        "Correction statement.",
                        List.of("repository metadata")
                ),
                CanonicalReport.PrivacyScope.PUBLIC_PLUS_PRIVATE_AGGREGATES
        );

        ReportSectionPlanner planner = new ReportSectionPlanner();

        MarkdownReportRenderer markdownRenderer =
                new MarkdownReportRenderer();
        markdownRenderer.planner = planner;

        PdfReportRenderer pdfRenderer = new PdfReportRenderer();
        pdfRenderer.planner = planner;

        String markdown = markdownRenderer.render(
                report,
                MarkdownReportType.FULL_DEVELOPER_REPORT
        );

        byte[] pdf = pdfRenderer.render(
                report,
                MarkdownReportType.FULL_DEVELOPER_REPORT
        );

        String pdfText;
        try (var document = Loader.loadPDF(pdf)) {
            pdfText = new PDFTextStripper().getText(document);
        }

        for (String coreText : List.of(
                "Canonical parity summary",
                "Data coverage",
                "Backend service",
                "Technology analysis",
                "Java",
                "Activity",
                "Significant projects",
                "Representative project",
                "Methodology",
                "Measured evidence statement."
        )) {
            assertTrue(markdown.contains(coreText), "Markdown: " + coreText);
            assertTrue(pdfText.contains(coreText), "PDF: " + coreText);
        }

        assertTrue(pdfText.contains("PUBLIC_PLUS_PRIVATE_AGGREGATES"));
    }
}
