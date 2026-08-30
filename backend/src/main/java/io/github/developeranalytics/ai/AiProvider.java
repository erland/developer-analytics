package io.github.developeranalytics.ai;

import java.util.List;
import java.util.Optional;

public interface AiProvider {

    String providerId();

    boolean isConfigured();

    String modelId();

    Optional<ProjectClassificationResult> classifyProject(
            ProjectClassificationRequest request
    );

    Optional<ProjectSummaryResult> summariseProject(
            ProjectSummaryRequest request
    );

    Optional<TechnologyNormalisationResult> normaliseTechnologies(
            TechnologyNormalisationRequest request
    );

    Optional<RoleInferenceResult> inferRoles(
            RoleInferenceRequest request
    );


    Optional<UserInsightsResult> summariseUserInsights(
            UserInsightsRequest request
    );

    Optional<TechnologyHistorySummaryResult> summariseTechnologyHistory(
            TechnologyHistorySummaryRequest request
    );

    record ProjectClassificationRequest(
            String projectName,
            String description,
            List<String> technologies,
            List<String> existingCategories
    ) {}

    record ProjectClassificationResult(
            List<String> categories,
            double confidence,
            String rationale
    ) {}

    record ProjectSummaryRequest(
            String projectName,
            String description,
            List<String> technologies,
            List<String> observedSignals
    ) {}

    record ProjectSummaryResult(
            String summary
    ) {}

    record TechnologyNormalisationRequest(
            List<String> observedTechnologyNames
    ) {}

    record TechnologyNormalisationResult(
            List<NormalisedTechnology> technologies
    ) {}

    record NormalisedTechnology(
            String observedName,
            String canonicalName
    ) {}

    record RoleInferenceRequest(
            List<String> projectCategories,
            List<String> technologies,
            List<String> contributionSignals
    ) {}

    record RoleInferenceResult(
            List<InferredRole> roles
    ) {}

    record InferredRole(
            String role,
            double confidence,
            String rationale
    ) {}

    record UserInsightsRequest(
            List<UserTechnologySignal> technologies,
            List<UserProjectCategorySignal> projectCategories,
            int publicRepositoryCount,
            int privateRepositoryCount,
            int ownedRepositoryCount,
            int externalRepositoryCount,
            int totalContributions
    ) {}

    record UserTechnologySignal(
            String technology,
            String evidenceLevel,
            int evidenceScore,
            int projectCount,
            String firstObservedAt,
            String lastObservedAt
    ) {}

    record UserProjectCategorySignal(
            String category,
            int projectCount
    ) {}

    record UserInsightsResult(
            List<InferredRole> likelyRoles,
            String technicalFocus,
            String breadthDepthObservation,
            String technologyEvolutionSummary,
            String openSourceEngagementSummary
    ) {}

    record TechnologyHistorySummaryRequest(
            String technologyName,
            List<TechnologyHistoryPoint> history
    ) {}

    record TechnologyHistoryPoint(
            String period,
            int projectCount,
            int activityCount
    ) {}

    record TechnologyHistorySummaryResult(
            String summary
    ) {}
}
