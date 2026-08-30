package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.domain.report.CanonicalReport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReportExportPrivacyBoundaryTest {

    private final ReportPrivacyPolicy policy = new ReportPrivacyPolicy();

    @Test
    void publicOnlyReportCannotIncludePrivateRepositoryNamesOrDetail() {
        SourceRepository privateRepository =
                repository("private-secret-repository", RepositoryVisibility.PRIVATE);

        assertFalse(policy.includeInAggregates(
                privateRepository,
                CanonicalReport.PrivacyScope.PUBLIC_ONLY
        ));
        assertFalse(policy.includeInProjectDetail(
                privateRepository,
                CanonicalReport.PrivacyScope.PUBLIC_ONLY
        ));

        String exposedName = policy.projectName(
                privateRepository,
                true,
                1
        );
        assertNotEquals("private-secret-repository", exposedName);
        assertEquals("Private repository 1", exposedName);
    }

    @Test
    void aggregatedPrivateReportIncludesPrivateDataButMasksNamesAndNoDetail() {
        SourceRepository privateRepository =
                repository("private-client-alpha", RepositoryVisibility.PRIVATE);

        assertTrue(policy.includeInAggregates(
                privateRepository,
                CanonicalReport.PrivacyScope.PUBLIC_PLUS_PRIVATE_AGGREGATES
        ));
        assertFalse(policy.includeInProjectDetail(
                privateRepository,
                CanonicalReport.PrivacyScope.PUBLIC_PLUS_PRIVATE_AGGREGATES
        ));

        assertEquals(
                "Private repository 3",
                policy.projectName(privateRepository, true, 3)
        );
    }

    @Test
    void fullPrivateReportContainsOnlyAuthorisedIncludedPrivateRepositories() {
        SourceRepository authorised =
                repository("authorised-private", RepositoryVisibility.PRIVATE);
        SourceRepository notAuthorised =
                repository("not-authorised-private", RepositoryVisibility.PRIVATE);
        notAuthorised.excludeFromAnalysis();

        assertTrue(policy.includeInAggregates(
                authorised,
                CanonicalReport.PrivacyScope.FULL_PRIVATE_DETAIL
        ));
        assertTrue(policy.includeInProjectDetail(
                authorised,
                CanonicalReport.PrivacyScope.FULL_PRIVATE_DETAIL
        ));

        assertFalse(policy.includeInAggregates(
                notAuthorised,
                CanonicalReport.PrivacyScope.FULL_PRIVATE_DETAIL
        ));
        assertFalse(policy.includeInProjectDetail(
                notAuthorised,
                CanonicalReport.PrivacyScope.FULL_PRIVATE_DETAIL
        ));
    }

    private SourceRepository repository(
            String name,
            RepositoryVisibility visibility
    ) {
        SourceRepository repository =
                new SourceRepository(null, "github", name, "owner", name);
        repository.setVisibility(visibility);
        repository.includeInAnalysis();
        return repository;
    }
}
