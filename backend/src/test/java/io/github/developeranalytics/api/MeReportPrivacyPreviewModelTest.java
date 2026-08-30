package io.github.developeranalytics.api;

import io.github.developeranalytics.service.report.MarkdownReportType;
import io.github.developeranalytics.service.report.ReportExportService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeReportPrivacyPreviewModelTest {

    @Test
    void exportRequiresSeparateExplicitGenerationConfirmationField() {
        var request = new MeReportExportResource.ExportRequest(
                MeReportExportResource.OutputFormat.MARKDOWN,
                MarkdownReportType.FULL_DEVELOPER_REPORT,
                ReportExportService.PrivateDataMode.EXCLUDE_PRIVATE,
                true,
                true
        );

        assertTrue(request.generationConfirmed());
        assertNotNull(request.reportType());
        assertNotNull(request.privateDataMode());
    }

    @Test
    void previewDoesNotCarryGenerationConfirmation() {
        var fields = java.util.Arrays.stream(
                MeReportExportResource.PreviewRequest.class
                        .getRecordComponents()
        )
        .map(component -> component.getName())
        .toList();

        assertFalse(fields.contains("generationConfirmed"));
    }
}
