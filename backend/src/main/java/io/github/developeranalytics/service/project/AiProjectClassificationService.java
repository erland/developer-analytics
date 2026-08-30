package io.github.developeranalytics.service.project;

import io.github.developeranalytics.ai.*;
import io.github.developeranalytics.domain.model.DataPrivacyProvenance;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.domain.project.AiProjectClassification;
import io.github.developeranalytics.domain.project.RepositoryProjectCategory;
import io.github.developeranalytics.domain.technology.RepositoryTechnologyEvidence;
import io.github.developeranalytics.persistence.project.AiProjectClassificationRepository;
import io.github.developeranalytics.persistence.project.RepositoryProjectCategoryRepository;
import io.github.developeranalytics.persistence.technology.RepositoryTechnologyEvidenceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@ApplicationScoped
public class AiProjectClassificationService {

    public static final String ANALYSIS_VERSION = "project-ai-v1";

    @Inject AiAnalysisGateway ai;
    @Inject AiProjectClassificationRepository aiClassifications;
    @Inject RepositoryProjectCategoryRepository deterministicClassifications;
    @Inject RepositoryTechnologyEvidenceRepository technologyEvidence;

    @Transactional
    public Result classify(SourceRepository repository) {
        AiAnalysisGateway.Availability availability = ai.availability();
        if (!availability.configured()) {
            return Result.unavailable();
        }

        List<RepositoryTechnologyEvidence> evidence =
                technologyEvidence.findForRepository(
                        repository.getUser().getId(),
                        repository.getId()
                );

        List<String> technologies = evidence.stream()
                .map(item -> item.getTechnology().getTechnologyKey())
                .distinct()
                .sorted()
                .toList();

        List<String> deterministic = deterministicClassifications
                .findForRepository(repository.getId())
                .stream()
                .filter(item -> item.getSource() ==
                        RepositoryProjectCategory.Source.DETERMINISTIC)
                .map(item -> item.getCategory().getCategoryKey())
                .distinct()
                .sorted()
                .toList();

        String fingerprint = fingerprint(
                repository,
                technologies,
                deterministic
        );

        Optional<AiProjectClassification> reusable =
                aiClassifications.findReusable(
                        repository.getId(),
                        fingerprint,
                        ANALYSIS_VERSION,
                        availability.providerId(),
                        availability.modelId()
                );

        if (reusable.isPresent()) {
            return Result.reused(reusable.get());
        }

        AiDataSensitivity sensitivity =
                repository.getVisibility() == RepositoryVisibility.PRIVATE
                        ? AiDataSensitivity.PRIVATE_METADATA
                        : AiDataSensitivity.PUBLIC_DATA;

        Optional<AiProvider.ProjectClassificationResult> generated =
                ai.classifyProject(
                        new AiRequestContext(
                                repository.getUser().getId(),
                                sensitivity
                        ),
                        new AiProvider.ProjectClassificationRequest(
                                repository.getName(),
                                repository.getDescription(),
                                technologies,
                                deterministic
                        )
                );

        if (generated.isEmpty()) {
            return Result.notGenerated();
        }

        AiProvider.ProjectClassificationResult result =
                generated.get();

        // AI may complement deterministic classification, but cannot invent
        // an unbounded taxonomy in this step: store only nonblank labels.
        List<String> classifications = result.categories().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        AiProjectClassification stored =
                new AiProjectClassification(
                        repository,
                        fingerprint,
                        classifications,
                        Math.max(0, Math.min(1, result.confidence())),
                        result.rationale(),
                        ANALYSIS_VERSION,
                        availability.providerId(),
                        availability.modelId(),
                        DataPrivacyProvenance.fromVisibility(
                                repository.getVisibility()
                        ),
                        OffsetDateTime.now(ZoneOffset.UTC)
                );

        aiClassifications.persist(stored);
        return Result.created(stored);
    }

    private String fingerprint(
            SourceRepository repository,
            List<String> technologies,
            List<String> deterministic
    ) {
        String source = String.join("\n",
                nullSafe(repository.getName()),
                nullSafe(repository.getDescription()),
                repository.getVisibility().name(),
                String.join(",", repository.getTopics().stream().sorted().toList()),
                String.join(",", technologies),
                String.join(",", deterministic)
        );

        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to fingerprint AI classification input",
                    exception
            );
        }
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    public record Result(
            Status status,
            AiProjectClassification classification
    ) {
        public enum Status {
            CREATED,
            REUSED,
            NOT_GENERATED,
            PROVIDER_UNAVAILABLE
        }

        static Result created(AiProjectClassification value) {
            return new Result(Status.CREATED, value);
        }

        static Result reused(AiProjectClassification value) {
            return new Result(Status.REUSED, value);
        }

        static Result notGenerated() {
            return new Result(Status.NOT_GENERATED, null);
        }

        static Result unavailable() {
            return new Result(Status.PROVIDER_UNAVAILABLE, null);
        }
    }
}
