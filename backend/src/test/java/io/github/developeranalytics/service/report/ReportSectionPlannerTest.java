package io.github.developeranalytics.service.report;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ReportSectionPlannerTest {

    @Test
    void markdownAndPdfCanShareOneSectionPlan() {
        var planner = new ReportSectionPlanner();
        var full = planner.sections(
                MarkdownReportType.FULL_DEVELOPER_REPORT
        );

        assertTrue(full.contains(ReportSection.DATA_COVERAGE));
        assertTrue(full.contains(ReportSection.TECHNOLOGY_ANALYSIS));
        assertTrue(full.contains(ReportSection.ACTIVITY));
        assertTrue(full.contains(ReportSection.ROLE_AI_ASSESSMENT));
        assertTrue(full.contains(ReportSection.METHODOLOGY));
    }
}
