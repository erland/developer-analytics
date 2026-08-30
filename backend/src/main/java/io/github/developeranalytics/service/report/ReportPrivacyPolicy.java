package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.domain.report.CanonicalReport;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ReportPrivacyPolicy {

    public boolean includeInAggregates(
            SourceRepository repository,
            CanonicalReport.PrivacyScope scope
    ) {
        if (!repository.isIncludedInAnalysis()) {
            return false;
        }
        if (repository.getVisibility() == RepositoryVisibility.PUBLIC) {
            return true;
        }
        return scope != CanonicalReport.PrivacyScope.PUBLIC_ONLY;
    }

    public boolean includeInProjectDetail(
            SourceRepository repository,
            CanonicalReport.PrivacyScope scope
    ) {
        if (!repository.isIncludedInAnalysis()) {
            return false;
        }
        if (repository.getVisibility() == RepositoryVisibility.PUBLIC) {
            return true;
        }
        return scope == CanonicalReport.PrivacyScope.FULL_PRIVATE_DETAIL;
    }

    public String projectName(
            SourceRepository repository,
            boolean hidePrivateRepositoryNames,
            int privateIndex
    ) {
        if (repository.getVisibility() == RepositoryVisibility.PRIVATE
                && hidePrivateRepositoryNames) {
            return "Private repository " + privateIndex;
        }
        return repository.getName();
    }
}
