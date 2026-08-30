package io.github.developeranalytics.ai;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import io.github.developeranalytics.observability.StructuredLog;

import java.util.Optional;
import java.util.function.Function;

@ApplicationScoped
public class AiAnalysisGateway {

    private static final Logger LOG =
            Logger.getLogger(AiAnalysisGateway.class);

    @Inject
    AiProvider provider;

    @Inject
    AiPrivacyPolicyService privacy;

    public Availability availability() {
        return new Availability(
                provider.providerId(),
                provider.modelId(),
                provider.isConfigured()
        );
    }

    public Optional<AiProvider.ProjectClassificationResult> classifyProject(
            AiRequestContext context,
            AiProvider.ProjectClassificationRequest request
    ) {
        return invoke(
                "classifyProject",
                context,
                request,
                provider::classifyProject
        );
    }

    public Optional<AiProvider.ProjectSummaryResult> summariseProject(
            AiRequestContext context,
            AiProvider.ProjectSummaryRequest request
    ) {
        return invoke(
                "summariseProject",
                context,
                request,
                provider::summariseProject
        );
    }

    public Optional<AiProvider.TechnologyNormalisationResult> normaliseTechnologies(
            AiRequestContext context,
            AiProvider.TechnologyNormalisationRequest request
    ) {
        return invoke(
                "normaliseTechnologies",
                context,
                request,
                provider::normaliseTechnologies
        );
    }

    public Optional<AiProvider.RoleInferenceResult> inferRoles(
            AiRequestContext context,
            AiProvider.RoleInferenceRequest request
    ) {
        return invoke(
                "inferRoles",
                context,
                request,
                provider::inferRoles
        );
    }

    public Optional<AiProvider.UserInsightsResult> summariseUserInsights(
            AiRequestContext context,
            AiProvider.UserInsightsRequest request
    ) {
        return invoke(
                "summariseUserInsights",
                context,
                request,
                provider::summariseUserInsights
        );
    }

    public Optional<AiProvider.TechnologyHistorySummaryResult> summariseTechnologyHistory(
            AiRequestContext context,
            AiProvider.TechnologyHistorySummaryRequest request
    ) {
        return invoke(
                "summariseTechnologyHistory",
                context,
                request,
                provider::summariseTechnologyHistory
        );
    }

    private <T, R> Optional<R> invoke(
            String requestType,
            AiRequestContext context,
            T request,
            Function<T, Optional<R>> operation
    ) {
        if (!provider.isConfigured()) {
            return Optional.empty();
        }

        AiPrivacyPolicyService.Decision decision =
                privacy.evaluate(context);

        if (!decision.allowed()) {
            LOG.infof(
                    "AI request blocked type=%s sensitivity=%s reason=%s",
                    requestType,
                    context.sensitivity(),
                    decision.reason()
            );
            return Optional.empty();
        }

        try {
            return operation.apply(request);
        } catch (RuntimeException failure) {
            StructuredLog.warn(
                    LOG,
                    "ai_request_unavailable",
                    failure,
                    StructuredLog.fields(
                            "requestType", requestType,
                            "provider", provider.providerId(),
                            "model", provider.modelId()
                    )
            );
            return Optional.empty();
        }
    }

    public record Availability(
            String providerId,
            String modelId,
            boolean configured
    ) {}
}
