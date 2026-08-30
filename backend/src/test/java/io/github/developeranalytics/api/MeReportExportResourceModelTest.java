package io.github.developeranalytics.api;

import io.github.developeranalytics.service.report.ReportExportService;
import io.github.developeranalytics.service.report.MarkdownReportType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeReportExportResourceModelTest {

    @Test
    void requestKeepsFormatPrivacyAndNameHandlingSeparate() {
        var request = new MeReportExportResource.ExportRequest(
                MeReportExportResource.OutputFormat.MARKDOWN,
                MarkdownReportType.FULL_DEVELOPER_REPORT,
                ReportExportService.PrivateDataMode
                        .INCLUDE_PRIVATE_AGGREGATES,
                true,
                true
        );

        assertEquals(
                MeReportExportResource.OutputFormat.MARKDOWN,
                request.outputFormat()
        );
        assertEquals(
                MarkdownReportType.FULL_DEVELOPER_REPORT,
                request.reportType()
        );
        assertEquals(
                ReportExportService.PrivateDataMode
                        .INCLUDE_PRIVATE_AGGREGATES,
                request.privateDataMode()
        );
        assertTrue(request.hidePrivateRepositoryNames());
        assertTrue(request.generationConfirmed());
    }
}
