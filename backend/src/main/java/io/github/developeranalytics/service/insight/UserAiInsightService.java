package io.github.developeranalytics.service.insight;

import io.github.developeranalytics.ai.*;
import io.github.developeranalytics.domain.insight.UserAiInsight;
import io.github.developeranalytics.domain.model.*;
import io.github.developeranalytics.domain.technology.UserTechnologyAssessment;
import io.github.developeranalytics.persistence.insight.UserAiInsightRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.persistence.technology.UserTechnologyAssessmentRepository;
import io.github.developeranalytics.service.correction.UserCorrectionService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@ApplicationScoped
public class UserAiInsightService {

    public static final String ANALYSIS_VERSION = "user-ai-v1";

    @Inject AiAnalysisGateway ai;
    @Inject UserAiInsightRepository insights;
    @Inject SourceRepositoryRepository repositories;
    @Inject UserTechnologyAssessmentRepository technologies;
    @Inject EntityManager entityManager;
    @Inject UserCorrectionService corrections;

    @Transactional
    public Result generate(AppUser user) {
        AiAnalysisGateway.Availability availability = ai.availability();
        if (!availability.configured()) {
            return Result.unavailable();
        }

        boolean allowPrivateMetadata =
                user.getAiPrivacyPolicy() ==
                        AiPrivacyPolicy.PRIVATE_METADATA_ALLOWED;

        List<SourceRepository> includedRepositories =
                repositories.findAllForUser(user.getId()).stream()
                        .filter(SourceRepository::isIncludedInAnalysis)
                        .filter(repository -> !corrections.isProjectExcludedFromAiProfile(
                                user.getId(),
                                repository.getId()
                        ))
                        .filter(repository ->
                                allowPrivateMetadata ||
                                repository.getVisibility() ==
                                        RepositoryVisibility.PUBLIC)
                        .toList();

        int publicCount = (int) includedRepositories.stream()
                .filter(repository ->
                        repository.getVisibility() ==
                                RepositoryVisibility.PUBLIC)
                .count();
        int privateCount = includedRepositories.size() - publicCount;

        int ownedCount = (int) includedRepositories.stream()
                .filter(repository ->
                        repository.getOwnershipRelation() ==
                                RepositoryOwnershipRelation.OWNED_BY_USER)
                .count();
        int externalCount = includedRepositories.size() - ownedCount;

        List<UserTechnologyAssessment> assessments =
                technologies.findForUser(user.getId()).stream()
                        .filter(assessment -> !corrections.isTechnologySuppressed(
                                user.getId(),
                                assessment.getTechnology().getTechnologyKey()
                        ))
                        .filter(assessment ->
                                allowPrivateMetadata ||
                                assessment.getPrivacyProvenance() ==
                                        DataPrivacyProvenance.PUBLIC_ONLY)
                        .limit(20)
                        .toList();

        List<AiProvider.UserTechnologySignal> technologySignals =
                assessments.stream()
                        .map(assessment ->
                                new AiProvider.UserTechnologySignal(
                                        assessment.getTechnology().getDisplayName(),
                                        assessment.getStrength().name(),
                                        assessment.getScore(),
                                        assessment.getRepositoryCount(),
                                        text(assessment.getFirstObservedAt()),
                                        text(assessment.getLastObservedAt())
                                ))
                        .toList();

        List<AiProvider.UserProjectCategorySignal> categories =
                categorySignals(
                        user.getId(),
                        allowPrivateMetadata,
                        includedRepositories.stream()
                                .map(SourceRepository::getId)
                                .toList()
                );

        int totalContributions =
                totalContributions(
                        user.getId(),
                        allowPrivateMetadata,
                        includedRepositories.stream()
                                .map(SourceRepository::getId)
                                .toList()
                );

        String fingerprint = fingerprint(
                technologySignals,
                categories,
                publicCount,
                privateCount,
                ownedCount,
                externalCount,
                totalContributions,
                user.getAiPrivacyPolicy().name()
        );

        Optional<UserAiInsight> reusable = insights.findReusable(
                user.getId(),
                fingerprint,
                ANALYSIS_VERSION,
                availability.providerId(),
                availability.modelId()
        );

        if (reusable.isPresent()) {
            return Result.reused(reusable.get());
        }

        AiDataSensitivity sensitivity =
                privateCount > 0
                        ? AiDataSensitivity.PRIVATE_METADATA
                        : AiDataSensitivity.PUBLIC_DATA;

        Optional<AiProvider.UserInsightsResult> generated =
                ai.summariseUserInsights(
                        new AiRequestContext(
                                user.getId(),
                                sensitivity
                        ),
                        new AiProvider.UserInsightsRequest(
                                technologySignals,
                                categories,
                                publicCount,
                                privateCount,
                                ownedCount,
                                externalCount,
                                totalContributions
                        )
                );

        if (generated.isEmpty()) {
            return Result.notGenerated();
        }

        AiProvider.UserInsightsResult value = generated.get();

        List<UserAiInsight.Role> roles = value.likelyRoles().stream()
                .map(role -> new UserAiInsight.Role(
                        role.role(),
                        Math.max(0, Math.min(1, role.confidence())),
                        role.rationale()
                ))
                .toList();

        DataPrivacyProvenance provenance =
                DataPrivacyProvenance.fromRepositoryCounts(
                        publicCount,
                        privateCount
                );

        UserAiInsight stored = new UserAiInsight(
                user,
                fingerprint,
                roles,
                value.technicalFocus(),
                value.breadthDepthObservation(),
                value.technologyEvolutionSummary(),
                value.openSourceEngagementSummary(),
                ANALYSIS_VERSION,
                availability.providerId(),
                availability.modelId(),
                provenance,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        insights.persist(stored);
        return Result.created(stored);
    }

    private List<AiProvider.UserProjectCategorySignal> categorySignals(
            UUID userId,
            boolean allowPrivateMetadata,
            List<UUID> repositoryIds
    ) {
        if (repositoryIds.isEmpty()) {
            return List.of();
        }
        String privacyClause = allowPrivateMetadata
                ? ""
                : " and c.repository.visibility=:publicVisibility ";

        var query = entityManager.createQuery(
                "select c.category.displayName, count(distinct c.repository.id) " +
                "from RepositoryProjectCategory c " +
                "where c.repository.user.id=:userId " +
                "and c.repository.id in :repositoryIds " +
                "and c.repository.includedInAnalysis=true " +
                "and not exists (" +
                "select 1 from UserAnalysisCorrection correction " +
                "where correction.user.id=:userId " +
                "and correction.repository.id=c.repository.id " +
                "and correction.type=io.github.developeranalytics.domain.correction.UserAnalysisCorrection.Type.PROJECT_CATEGORY_REJECTED " +
                "and correction.correctionKey=c.category.categoryKey" +
                ") " +
                privacyClause +
                "group by c.category.displayName " +
                "order by count(distinct c.repository.id) desc",
                Object[].class
        ).setParameter("userId", userId)
          .setParameter("repositoryIds", repositoryIds);

        if (!allowPrivateMetadata) {
            query.setParameter(
                    "publicVisibility",
                    RepositoryVisibility.PUBLIC
            );
        }

        return query.setMaxResults(15)
                .getResultList()
                .stream()
                .map(row -> new AiProvider.UserProjectCategorySignal(
                        (String) row[0],
                        ((Number) row[1]).intValue()
                ))
                .toList();
    }

    private int totalContributions(
            UUID userId,
            boolean allowPrivateMetadata,
            List<UUID> repositoryIds
    ) {
        if (repositoryIds.isEmpty()) {
            return 0;
        }
        String privacyClause = allowPrivateMetadata
                ? ""
                : " and c.repository.visibility=:publicVisibility ";

        var query = entityManager.createQuery(
                "select count(c.id) from Contribution c " +
                "where c.user.id=:userId " +
                "and c.repository.id in :repositoryIds " +
                "and c.repository.includedInAnalysis=true " +
                privacyClause,
                Long.class
        ).setParameter("userId", userId)
          .setParameter("repositoryIds", repositoryIds);

        if (!allowPrivateMetadata) {
            query.setParameter(
                    "publicVisibility",
                    RepositoryVisibility.PUBLIC
            );
        }

        return Math.toIntExact(query.getSingleResult());
    }

    private String fingerprint(Object... values) {
        String source = Arrays.deepToString(values);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to fingerprint user AI insight input",
                    exception
            );
        }
    }

    private String text(OffsetDateTime value) {
        return value == null ? "" : value.toString();
    }

    public record Result(Status status, UserAiInsight insight) {
        public enum Status {
            CREATED,
            REUSED,
            NOT_GENERATED,
            PROVIDER_UNAVAILABLE
        }

        static Result created(UserAiInsight insight) {
            return new Result(Status.CREATED, insight);
        }

        static Result reused(UserAiInsight insight) {
            return new Result(Status.REUSED, insight);
        }

        static Result notGenerated() {
            return new Result(Status.NOT_GENERATED, null);
        }

        static Result unavailable() {
            return new Result(Status.PROVIDER_UNAVAILABLE, null);
        }
    }
}
