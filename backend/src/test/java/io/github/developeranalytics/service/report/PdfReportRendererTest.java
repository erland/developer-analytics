package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.report.CanonicalReport;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PdfReportRendererTest {

    @Test
    void pdfUsesCanonicalModelAndStartsWithPdfSignature() throws IOException {
        CanonicalReport report = new CanonicalReport(
                CanonicalReport.MODEL_VERSION,
                OffsetDateTime.parse("2026-08-30T10:00:00Z"),
                new CanonicalReport.Summary("Report", "Canonical PDF summary"),
                new CanonicalReport.Period(null, null),
                new CanonicalReport.DataCoverage(1, 1, 0, 1, 2),
                List.of(),
                List.of(),
                new CanonicalReport.Activity(
                        2,
                        Map.of("COMMIT", 2),
                        List.of(new CanonicalReport.ActivityMonth(
                                "2026-08", 2, 1
                        ))
                ),
                List.of(),
                CanonicalReport.RoleAiAssessment.unavailable(),
                new CanonicalReport.Methodology(
                        "Measured.", "Inference.", "Corrections.", List.of()
                ),
                CanonicalReport.PrivacyScope.PUBLIC_ONLY
        );

        PdfReportRenderer renderer = new PdfReportRenderer();
        renderer.planner = new ReportSectionPlanner();

        byte[] pdf = renderer.render(
                report,
                MarkdownReportType.ACTIVITY_REPORT
        );

        assertTrue(pdf.length > 100);
        assertEquals("%PDF", new String(pdf, 0, 4));

        try (var document = Loader.loadPDF(pdf)) {
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("Privacy: PUBLIC_ONLY"));
            assertTrue(text.contains("Canonical PDF summary"));
        }
    }
}
