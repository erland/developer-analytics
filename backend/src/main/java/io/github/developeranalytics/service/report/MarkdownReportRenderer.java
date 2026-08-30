package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.report.CanonicalReport;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MarkdownReportRenderer {

    @jakarta.inject.Inject
    ReportSectionPlanner planner = new ReportSectionPlanner();


    public String render(
            CanonicalReport report,
            MarkdownReportType reportType
    ) {
        StringBuilder out = new StringBuilder();

        out.append("# ").append(reportType.title()).append("\n\n");
        out.append(report.summary().overview()).append("\n\n");

        for (ReportSection section : planner.sections(reportType)) {
            switch (section) {
                case METADATA -> appendMetadata(out, report, reportType);
                case DATA_COVERAGE -> appendDataCoverage(out, report);
                case PROJECT_CATEGORIES -> appendProjectCategories(out, report);
                case TECHNOLOGY_ANALYSIS -> appendTechnologyAnalysis(out, report);
                case ACTIVITY -> appendActivity(out, report);
                case SIGNIFICANT_PROJECTS -> appendSignificantProjects(out, report);
                case ROLE_AI_ASSESSMENT -> appendRoleAiAssessment(out, report);
                case METHODOLOGY -> appendMethodology(out, report);
            }
        }

        return out.toString();
    }

    private void appendMetadata(
            StringBuilder out,
            CanonicalReport report,
            MarkdownReportType reportType
    ) {
        out.append("## Report metadata\n\n");
        out.append("- Report type: `").append(reportType).append("`\n");
        out.append("- Report model: `").append(report.modelVersion()).append("`\n");
        out.append("- Generated: ").append(report.generatedAt()).append("\n");
        out.append("- Privacy scope: `").append(report.privacyScope()).append("`\n");
        out.append("- Period: ")
                .append(report.period().firstActivityAt() == null
                        ? "No recorded activity"
                        : report.period().firstActivityAt())
                .append(" – ")
                .append(report.period().lastActivityAt() == null
                        ? "No recorded activity"
                        : report.period().lastActivityAt())
                .append("\n\n");
    }

    private void appendDataCoverage(StringBuilder out, CanonicalReport report) {
        out.append("## Data coverage\n\n");
        out.append("- Repositories: ").append(report.dataCoverage().repositoryCount()).append("\n");
        out.append("- Public repositories: ").append(report.dataCoverage().publicRepositoryCount()).append("\n");
        out.append("- Private repositories represented: ").append(report.dataCoverage().privateRepositoryCount()).append("\n");
        out.append("- Repositories available as project detail: ").append(report.dataCoverage().repositoriesIncludedInDetail()).append("\n");
        out.append("- Contributions: ").append(report.dataCoverage().contributionCount()).append("\n\n");
    }

    private void appendProjectCategories(StringBuilder out, CanonicalReport report) {
        out.append("## Project categories\n\n");
        if (report.projectCategories().isEmpty()) {
            out.append("No project-category analysis available.\n\n");
            return;
        }
        out.append("| Category | Projects |\n| --- | ---: |\n");
        report.projectCategories().forEach(item ->
                out.append("| ").append(escape(item.name()))
                        .append(" | ").append(item.projectCount()).append(" |\n"));
        out.append("\n");
    }

    private void appendTechnologyAnalysis(StringBuilder out, CanonicalReport report) {
        out.append("## Technology analysis\n\n");
        if (report.technologyAnalysis().isEmpty()) {
            out.append("No technology analysis available.\n\n");
            return;
        }
        out.append("| Technology | Evidence | Score | Projects | First observed | Latest observed | Privacy |\n");
        out.append("| --- | --- | ---: | ---: | --- | --- | --- |\n");
        report.technologyAnalysis().forEach(item ->
                out.append("| ").append(escape(item.name()))
                        .append(" | ").append(item.evidenceLevel())
                        .append(" | ").append(item.evidenceScore())
                        .append(" | ").append(item.projectCount())
                        .append(" | ").append(item.firstObservedAt() == null ? "—" : item.firstObservedAt())
                        .append(" | ").append(item.lastObservedAt() == null ? "—" : item.lastObservedAt())
                        .append(" | ").append(item.privacyProvenance())
                        .append(" |\n"));
        out.append("\n");
    }

    private void appendActivity(StringBuilder out, CanonicalReport report) {
        out.append("## Activity\n\n");
        out.append("### Contribution totals\n\n");
        report.activity().byType().forEach((type,count) ->
                out.append("- ").append(type).append(": ").append(count).append("\n"));
        out.append("\n");

        out.append("### Monthly activity\n\n");
        if (report.activity().monthly().isEmpty()) {
            out.append("No monthly activity available.\n\n");
            return;
        }
        out.append("| Month | Contributions | Active projects |\n");
        out.append("| --- | ---: | ---: |\n");
        report.activity().monthly().forEach(month ->
                out.append("| ").append(month.month())
                        .append(" | ").append(month.contributionCount())
                        .append(" | ").append(month.activeProjectCount())
                        .append(" |\n"));
        out.append("\n");
    }

    private void appendSignificantProjects(StringBuilder out, CanonicalReport report) {
        out.append("## Significant projects\n\n");
        if (report.significantProjects().isEmpty()) {
            out.append("No significant projects available for this privacy scope.\n\n");
            return;
        }
        out.append("| Project | Visibility | Significance | Involvement |\n");
        out.append("| --- | --- | --- | --- |\n");
        report.significantProjects().forEach(project ->
                out.append("| ").append(escape(project.repositoryName()))
                        .append(" | ").append(project.visibility())
                        .append(" | ").append(project.significanceLevel())
                        .append(" (").append(project.significanceScore()).append(")")
                        .append(" | ").append(project.involvementLevel())
                        .append(" (").append(project.involvementScore()).append(")")
                        .append(" |\n"));
        out.append("\n");
    }

    private void appendRoleAiAssessment(StringBuilder out, CanonicalReport report) {
        out.append("## Role / AI assessment\n\n");
        if (!report.roleAiAssessment().available()) {
            out.append("No AI-generated user-level assessment is included in this report.\n\n");
            return;
        }
        out.append("**AI-generated interpretation.**\n\n");
        out.append("- Technical focus: ").append(report.roleAiAssessment().technicalFocus()).append("\n");
        out.append("- Breadth/depth: ").append(report.roleAiAssessment().breadthDepthObservation()).append("\n");
        out.append("- Technology evolution: ").append(report.roleAiAssessment().technologyEvolutionSummary()).append("\n");
        out.append("- Open-source engagement: ").append(report.roleAiAssessment().openSourceEngagementSummary()).append("\n");
        if (!report.roleAiAssessment().likelyRoles().isEmpty()) {
            out.append("- Likely roles:\n");
            report.roleAiAssessment().likelyRoles().forEach(role ->
                    out.append("  - ").append(role.role())
                            .append(" (confidence ")
                            .append(Math.round(role.confidence() * 100))
                            .append("%): ")
                            .append(role.rationale()).append("\n"));
        }
        out.append("\n");
    }

    private void appendMethodology(StringBuilder out, CanonicalReport report) {
        out.append("## Methodology\n\n");
        out.append(report.methodology().measuredDataStatement()).append("\n\n");
        out.append(report.methodology().inferenceStatement()).append("\n\n");
        out.append(report.methodology().correctionStatement()).append("\n\n");
        if (!report.methodology().sourceTypes().isEmpty()) {
            out.append("### Source types\n\n");
            report.methodology().sourceTypes().forEach(source ->
                    out.append("- ").append(source).append("\n"));
            out.append("\n");
        }
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("|", "\\|").replace("\n", " ");
    }
}
