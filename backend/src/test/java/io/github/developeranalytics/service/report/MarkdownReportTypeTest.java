package io.github.developeranalytics.service.report;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownReportTypeTest {
    @Test
    void supportsRequiredCanonicalMarkdownExports() {
        assertEquals(4, MarkdownReportType.values().length);
        assertEquals(
                "developer-analytics-public-oss-report.md",
                MarkdownReportType.PUBLIC_OSS_REPORT.filename()
        );
        assertEquals(
                "developer-analytics-full-report.md",
                MarkdownReportType.FULL_DEVELOPER_REPORT.filename()
        );
    }
}
