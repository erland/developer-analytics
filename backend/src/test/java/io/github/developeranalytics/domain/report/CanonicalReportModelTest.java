package io.github.developeranalytics.domain.report;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalReportModelTest {

    @Test
    void canonicalModelContainsRequiredReportSections() {
        CanonicalReport report = new CanonicalReport(
                CanonicalReport.MODEL_VERSION,
                OffsetDateTime.parse("2026-08-30T10:00:00Z"),
                new CanonicalReport.Summary("Report", "Summary"),
                new CanonicalReport.Period(null, null),
                new CanonicalReport.DataCoverage(0, 0, 0, 0, 0),
                List.of(),
                List.of(),
                new CanonicalReport.Activity(0, Map.of(), List.of()),
                List.of(),
                CanonicalReport.RoleAiAssessment.unavailable(),
                new CanonicalReport.Methodology(
                        "measured",
                        "inference",
                        "corrections",
                        List.of("repository metadata")
                ),
                CanonicalReport.PrivacyScope.PUBLIC_ONLY
        );

        assertEquals("report-v1", report.modelVersion());
        assertNotNull(report.summary());
        assertNotNull(report.period());
        assertNotNull(report.dataCoverage());
        assertNotNull(report.projectCategories());
        assertNotNull(report.technologyAnalysis());
        assertNotNull(report.activity());
        assertNotNull(report.significantProjects());
        assertNotNull(report.roleAiAssessment());
        assertNotNull(report.methodology());
        assertEquals(
                CanonicalReport.PrivacyScope.PUBLIC_ONLY,
                report.privacyScope()
        );
    }
}
