package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.report.CanonicalReport;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Tag("privacy")
@Tag("unit")
class ReportPrivacyPreviewTest {

    @Test
    void previewExposesRequiredPrivacyFacts() {
        var preview = new ReportExportService.PreviewResult(
                MarkdownReportType.FULL_DEVELOPER_REPORT,
                ReportExportService.PrivateDataMode.INCLUDE_PRIVATE_AGGREGATES,
                CanonicalReport.PrivacyScope.PUBLIC_PLUS_PRIVATE_AGGREGATES,
                true,
                false,
                true,
                OffsetDateTime.parse("2025-01-01T00:00:00Z"),
                OffsetDateTime.parse("2026-08-30T00:00:00Z"),
                12,
                10,
                2,
                1000,
                CanonicalReport.MODEL_VERSION
        );

        assertTrue(preview.privateRepositoriesIncluded());
        assertFalse(preview.privateNamesIncluded());
        assertTrue(preview.aiAssessmentsIncluded());
        assertEquals(
                CanonicalReport.PrivacyScope.PUBLIC_PLUS_PRIVATE_AGGREGATES,
                preview.privacyScope()
        );
        assertNotNull(preview.firstActivityAt());
        assertNotNull(preview.lastActivityAt());
    }
}
