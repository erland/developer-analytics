package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.change.ChangeKind;
import io.github.developeranalytics.domain.report.CanonicalReport;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ReportExportService {

    public enum PrivateDataMode {
        EXCLUDE_PRIVATE,
        INCLUDE_PRIVATE_AGGREGATES,
        INCLUDE_FULL_PRIVATE_DETAIL
    }

    @Inject CanonicalReportService reports;
    @Inject MarkdownReportRenderer markdownRenderer;
    @Inject PdfReportRenderer pdfRenderer;

    public PreviewResult preview(UUID userId, MarkdownReportType reportType, PrivateDataMode privateDataMode,
                                 boolean hidePrivateRepositoryNames) {
        return preview(userId, reportType, privateDataMode, hidePrivateRepositoryNames, Set.of());
    }

    public PreviewResult preview(UUID userId, MarkdownReportType reportType, PrivateDataMode privateDataMode,
                                 boolean hidePrivateRepositoryNames, Set<ChangeKind> changeKinds) {
        EffectiveSettings settings = effectiveSettings(reportType, privateDataMode, hidePrivateRepositoryNames);
        CanonicalReport report = reports.build(userId, settings.privacyScope(), hidePrivateRepositoryNames, changeKinds);
        boolean privateRepositoriesIncluded = report.dataCoverage().privateRepositoryCount() > 0;
        boolean privateNamesIncluded = settings.privateDataMode() == PrivateDataMode.INCLUDE_FULL_PRIVATE_DETAIL
                && privateRepositoriesIncluded && !hidePrivateRepositoryNames;
        return new PreviewResult(
                reportType, settings.privateDataMode(), report.privacyScope(), privateRepositoriesIncluded,
                privateNamesIncluded, report.roleAiAssessment().available(), report.period().firstActivityAt(),
                report.period().lastActivityAt(), report.dataCoverage().repositoryCount(),
                report.dataCoverage().publicRepositoryCount(), report.dataCoverage().privateRepositoryCount(),
                report.dataCoverage().contributionCount(), report.modelVersion(), report.changeScope());
    }

    public ExportResult exportMarkdown(UUID userId, MarkdownReportType reportType, PrivateDataMode privateDataMode,
                                       boolean hidePrivateRepositoryNames) {
        return exportMarkdown(userId, reportType, privateDataMode, hidePrivateRepositoryNames, Set.of());
    }

    public ExportResult exportMarkdown(UUID userId, MarkdownReportType reportType, PrivateDataMode privateDataMode,
                                       boolean hidePrivateRepositoryNames, Set<ChangeKind> changeKinds) {
        EffectiveSettings settings = effectiveSettings(reportType, privateDataMode, hidePrivateRepositoryNames);
        CanonicalReport report = reports.build(userId, settings.privacyScope(), hidePrivateRepositoryNames, changeKinds);
        return new ExportResult(markdownRenderer.render(report, reportType),
                report.dataCoverage().publicRepositoryCount(), report.dataCoverage().privateRepositoryCount(),
                settings.privateDataMode(), hidePrivateRepositoryNames, report.modelVersion(), reportType,
                reportType.filename(), report.changeScope());
    }

    public PdfExportResult exportPdf(UUID userId, MarkdownReportType reportType, PrivateDataMode privateDataMode,
                                     boolean hidePrivateRepositoryNames) {
        return exportPdf(userId, reportType, privateDataMode, hidePrivateRepositoryNames, Set.of());
    }

    public PdfExportResult exportPdf(UUID userId, MarkdownReportType reportType, PrivateDataMode privateDataMode,
                                     boolean hidePrivateRepositoryNames, Set<ChangeKind> changeKinds) {
        EffectiveSettings settings = effectiveSettings(reportType, privateDataMode, hidePrivateRepositoryNames);
        CanonicalReport report = reports.build(userId, settings.privacyScope(), hidePrivateRepositoryNames, changeKinds);
        return new PdfExportResult(pdfRenderer.render(report, reportType), settings.privateDataMode(),
                hidePrivateRepositoryNames, report.modelVersion(), reportType, pdfFilename(reportType), report.changeScope());
    }

    private String pdfFilename(MarkdownReportType reportType) {
        return reportType.filename().replace(".md", ".pdf");
    }

    private EffectiveSettings effectiveSettings(MarkdownReportType reportType, PrivateDataMode privateDataMode,
                                                boolean hidePrivateRepositoryNames) {
        Objects.requireNonNull(reportType, "reportType must be explicitly selected");
        Objects.requireNonNull(privateDataMode, "privateDataMode must be explicitly selected");
        PrivateDataMode effectivePrivateDataMode = reportType == MarkdownReportType.PUBLIC_OSS_REPORT
                ? PrivateDataMode.EXCLUDE_PRIVATE : privateDataMode;
        return new EffectiveSettings(effectivePrivateDataMode, toPrivacyScope(effectivePrivateDataMode));
    }

    private CanonicalReport.PrivacyScope toPrivacyScope(PrivateDataMode mode) {
        return switch (mode) {
            case EXCLUDE_PRIVATE -> CanonicalReport.PrivacyScope.PUBLIC_ONLY;
            case INCLUDE_PRIVATE_AGGREGATES -> CanonicalReport.PrivacyScope.PUBLIC_PLUS_PRIVATE_AGGREGATES;
            case INCLUDE_FULL_PRIVATE_DETAIL -> CanonicalReport.PrivacyScope.FULL_PRIVATE_DETAIL;
        };
    }

    private record EffectiveSettings(PrivateDataMode privateDataMode, CanonicalReport.PrivacyScope privacyScope) {}

    public record PreviewResult(
            MarkdownReportType reportType,
            PrivateDataMode privateDataMode,
            CanonicalReport.PrivacyScope privacyScope,
            boolean privateRepositoriesIncluded,
            boolean privateNamesIncluded,
            boolean aiAssessmentsIncluded,
            OffsetDateTime firstActivityAt,
            OffsetDateTime lastActivityAt,
            int repositoryCount,
            int publicRepositoryCount,
            int privateRepositoryCount,
            int contributionCount,
            String reportModelVersion,
            CanonicalReport.ChangeScope changeScope
    ) {}

    public record PdfExportResult(
            byte[] pdf,
            PrivateDataMode privateDataMode,
            boolean hidePrivateRepositoryNames,
            String reportModelVersion,
            MarkdownReportType reportType,
            String filename,
            CanonicalReport.ChangeScope changeScope
    ) {}

    public record ExportResult(
            String markdown,
            int publicRepositoryCount,
            int privateRepositoryCount,
            PrivateDataMode privateDataMode,
            boolean hidePrivateRepositoryNames,
            String reportModelVersion,
            MarkdownReportType reportType,
            String filename,
            CanonicalReport.ChangeScope changeScope
    ) {}
}
