package io.github.developeranalytics.ai;

import java.util.Optional;

public class DisabledAiProvider implements AiProvider {

    @Override
    public String providerId() {
        return "disabled";
    }

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String modelId() {
        return "none";
    }

    @Override
    public Optional<ProjectClassificationResult> classifyProject(
            ProjectClassificationRequest request
    ) {
        return Optional.empty();
    }

    @Override
    public Optional<ProjectSummaryResult> summariseProject(
            ProjectSummaryRequest request
    ) {
        return Optional.empty();
    }

    @Override
    public Optional<TechnologyNormalisationResult> normaliseTechnologies(
            TechnologyNormalisationRequest request
    ) {
        return Optional.empty();
    }

    @Override
    public Optional<RoleInferenceResult> inferRoles(
            RoleInferenceRequest request
    ) {
        return Optional.empty();
    }

    @Override
    public Optional<UserInsightsResult> summariseUserInsights(
            UserInsightsRequest request
    ) {
        return Optional.empty();
    }

    @Override
    public Optional<TechnologyHistorySummaryResult> summariseTechnologyHistory(
            TechnologyHistorySummaryRequest request
    ) {
        return Optional.empty();
    }
}
