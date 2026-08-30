package io.github.developeranalytics.domain.report;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record CanonicalReport(
        String modelVersion,
        OffsetDateTime generatedAt,
        Summary summary,
        Period period,
        DataCoverage dataCoverage,
        List<ProjectCategory> projectCategories,
        List<TechnologyAnalysis> technologyAnalysis,
        Activity activity,
        List<SignificantProject> significantProjects,
        RoleAiAssessment roleAiAssessment,
        Methodology methodology,
        PrivacyScope privacyScope
) {
    public static final String MODEL_VERSION = "report-v1";

    public enum PrivacyScope {
        PUBLIC_ONLY,
        PUBLIC_PLUS_PRIVATE_AGGREGATES,
        FULL_PRIVATE_DETAIL
    }

    public record Summary(
            String title,
            String overview
    ) {}

    public record Period(
            OffsetDateTime firstActivityAt,
            OffsetDateTime lastActivityAt
    ) {}

    public record DataCoverage(
            int repositoryCount,
            int publicRepositoryCount,
            int privateRepositoryCount,
            int repositoriesIncludedInDetail,
            int contributionCount
    ) {}

    public record ProjectCategory(
            String key,
            String name,
            int projectCount
    ) {}

    public record TechnologyAnalysis(
            String key,
            String name,
            String evidenceLevel,
            int evidenceScore,
            int projectCount,
            OffsetDateTime firstObservedAt,
            OffsetDateTime lastObservedAt,
            String privacyProvenance
    ) {}

    public record Activity(
            int contributionCount,
            Map<String, Integer> byType,
            List<ActivityMonth> monthly
    ) {}

    public record ActivityMonth(
            String month,
            int contributionCount,
            int activeProjectCount
    ) {}

    public record SignificantProject(
            UUID repositoryId,
            String repositoryName,
            String visibility,
            String ownership,
            String significanceLevel,
            int significanceScore,
            String involvementLevel,
            int involvementScore
    ) {}

    public record RoleAiAssessment(
            boolean available,
            boolean aiGenerated,
            List<Role> likelyRoles,
            String technicalFocus,
            String breadthDepthObservation,
            String technologyEvolutionSummary,
            String openSourceEngagementSummary,
            String providerId,
            String modelId,
            String privacyProvenance
    ) {
        public static RoleAiAssessment unavailable() {
            return new RoleAiAssessment(
                    false, false, List.of(),
                    "", "", "", "", null, null, "PUBLIC_ONLY"
            );
        }
    }

    public record Role(
            String role,
            double confidence,
            String rationale
    ) {}

    public record Methodology(
            String measuredDataStatement,
            String inferenceStatement,
            String correctionStatement,
            List<String> sourceTypes
    ) {}
}
