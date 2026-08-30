package io.github.developeranalytics.service.report;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ReportSectionPlanner {

    public List<ReportSection> sections(MarkdownReportType reportType) {
        return switch (reportType) {
            case PUBLIC_OSS_REPORT -> List.of(
                    ReportSection.METADATA,
                    ReportSection.DATA_COVERAGE,
                    ReportSection.PROJECT_CATEGORIES,
                    ReportSection.TECHNOLOGY_ANALYSIS,
                    ReportSection.ACTIVITY,
                    ReportSection.SIGNIFICANT_PROJECTS,
                    ReportSection.METHODOLOGY
            );
            case FULL_DEVELOPER_REPORT -> List.of(
                    ReportSection.METADATA,
                    ReportSection.DATA_COVERAGE,
                    ReportSection.PROJECT_CATEGORIES,
                    ReportSection.TECHNOLOGY_ANALYSIS,
                    ReportSection.ACTIVITY,
                    ReportSection.SIGNIFICANT_PROJECTS,
                    ReportSection.ROLE_AI_ASSESSMENT,
                    ReportSection.METHODOLOGY
            );
            case TECHNOLOGY_PROFILE -> List.of(
                    ReportSection.METADATA,
                    ReportSection.DATA_COVERAGE,
                    ReportSection.TECHNOLOGY_ANALYSIS,
                    ReportSection.ROLE_AI_ASSESSMENT,
                    ReportSection.METHODOLOGY
            );
            case ACTIVITY_REPORT -> List.of(
                    ReportSection.METADATA,
                    ReportSection.DATA_COVERAGE,
                    ReportSection.ACTIVITY,
                    ReportSection.METHODOLOGY
            );
        };
    }
}
