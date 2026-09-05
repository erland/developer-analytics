package io.github.developeranalytics.service.external;

import io.github.developeranalytics.domain.correction.UserAnalysisCorrection;
import io.github.developeranalytics.domain.external.ExternalClientToken;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.DataPrivacyProvenance;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.correction.UserAnalysisCorrectionRepository;
import io.github.developeranalytics.persistence.project.ProjectTypeAnalyticsRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.persistence.technology.UserTechnologyAssessmentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;

@ApplicationScoped
public class ExternalAnalysisApplicationService {

    @Inject SourceRepositoryRepository repositories;
    @Inject UserAnalysisCorrectionRepository corrections;
    @Inject UserTechnologyAssessmentRepository technologyAssessments;
    @Inject ProjectTypeAnalyticsRepository projectTypes;
    @Inject EntityManager entityManager;

    @Transactional
    public List<ProjectResult> projects(UUID userId, ExternalClientToken.PrivacyScope privacyScope, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return repositories.findAllForUser(userId).stream()
                .filter(SourceRepository::isIncludedInAnalysis)
                .filter(repository -> privacyScope.allowsPrivateProjectDetail()
                        || repository.getVisibility() == RepositoryVisibility.PUBLIC)
                .limit(safeLimit)
                .map(repository -> new ProjectResult(
                        repository.getId(), repository.getName(), repository.getVisibility().name(),
                        repository.getOwnershipRelation().name(), repository.getLastActivityAt(),
                        categoryKeys(repository.getId()), technologyKeys(repository.getId()),
                        excludedFromAiProfile(userId, repository.getId())))
                .toList();
    }

    @Transactional
    public List<TechnologySummaryResult> technologies(
            UUID userId,
            ExternalClientToken.PrivacyScope privacyScope,
            int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return technologyAssessments.findForUser(userId).stream()
                .filter(assessment -> privacyScope.allowsPrivateAggregates()
                        || assessment.getPrivacyProvenance() == DataPrivacyProvenance.PUBLIC_ONLY)
                .limit(safeLimit)
                .map(assessment -> new TechnologySummaryResult(
                        assessment.getTechnology().getTechnologyKey(),
                        assessment.getTechnology().getDisplayName(),
                        assessment.getStrength().name(),
                        assessment.getScore(),
                        assessment.getRepositoryCount(),
                        assessment.getFirstObservedAt(),
                        assessment.getLastObservedAt(),
                        assessment.getPrivacyProvenance().name()))
                .toList();
    }

    @Transactional
    public List<ProjectTypeSummaryResult> projectTypes(
            UUID userId,
            ExternalClientToken.PrivacyScope privacyScope,
            int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        if (privacyScope.allowsPrivateAggregates()) {
            return projectTypes.categorySummaries(userId).stream()
                    .limit(safeLimit)
                    .map(row -> new ProjectTypeSummaryResult(
                            row.categoryKey(),
                            row.categoryName(),
                            row.projectCount()))
                    .toList();
        }

        return entityManager.createQuery(
                "select c.category.categoryKey, c.category.displayName, " +
                "count(distinct c.repository.id) " +
                "from RepositoryProjectCategory c " +
                "where c.repository.user.id=:userId " +
                "and c.repository.includedInAnalysis=true " +
                "and c.repository.visibility=:publicVisibility " +
                "group by c.category.categoryKey, c.category.displayName " +
                "order by count(distinct c.repository.id) desc",
                Object[].class)
                .setParameter("userId", userId)
                .setParameter("publicVisibility", RepositoryVisibility.PUBLIC)
                .setMaxResults(safeLimit)
                .getResultList()
                .stream()
                .map(row -> new ProjectTypeSummaryResult(
                        (String) row[0],
                        (String) row[1],
                        ((Number) row[2]).intValue()))
                .toList();
    }

    @Transactional
    public EvidenceResult evidence(
            UUID userId,
            ExternalClientToken.PrivacyScope privacyScope,
            int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 200));

        List<TechnologyEvidenceResult> technologyEvidence = entityManager.createQuery(
                        "select e.technology.technologyKey, " +
                        "e.evidenceType, e.strength, count(e), " +
                        "e.privacyProvenance " +
                        "from RepositoryTechnologyEvidence e " +
                        "where e.repository.user.id=:userId " +
                        "and e.repository.includedInAnalysis=true " +
                        "group by e.technology.technologyKey, " +
                        "e.evidenceType, e.strength, e.privacyProvenance " +
                        "order by count(e) desc",
                        Object[].class)
                .setParameter("userId", userId)
                .setMaxResults(safeLimit)
                .getResultList()
                .stream()
                .filter(row -> privacyScope.allowsPrivateAggregates()
                        || DataPrivacyProvenance.PUBLIC_ONLY.name().equals(row[4].toString()))
                .map(row -> new TechnologyEvidenceResult(
                        (String) row[0],
                        row[1].toString(),
                        row[2].toString(),
                        ((Number) row[3]).intValue(),
                        row[4].toString()))
                .toList();

        List<CategoryEvidenceResult> categoryEvidence = entityManager.createQuery(
                        "select c.category.categoryKey, c.source, c.confidence, " +
                        "count(c), c.privacyProvenance " +
                        "from RepositoryProjectCategory c " +
                        "where c.repository.user.id=:userId " +
                        "and c.repository.includedInAnalysis=true " +
                        "group by c.category.categoryKey, c.source, " +
                        "c.confidence, c.privacyProvenance " +
                        "order by count(c) desc",
                        Object[].class)
                .setParameter("userId", userId)
                .setMaxResults(safeLimit)
                .getResultList()
                .stream()
                .filter(row -> privacyScope.allowsPrivateAggregates()
                        || DataPrivacyProvenance.PUBLIC_ONLY.name().equals(row[4].toString()))
                .map(row -> new CategoryEvidenceResult(
                        (String) row[0],
                        row[1].toString(),
                        row[2].toString(),
                        ((Number) row[3]).intValue(),
                        row[4].toString()))
                .toList();

        return new EvidenceResult(technologyEvidence, categoryEvidence);
    }

    @Transactional
    public ActivityResult activity(UUID userId, ExternalClientToken.PrivacyScope privacyScope, int months) {
        int safeMonths = Math.max(1, Math.min(months, 120));
        OffsetDateTime threshold = OffsetDateTime.now().minusMonths(safeMonths);
        String visibilityClause = privacyScope.allowsPrivateAggregates()
                ? "" : " and c.repository.visibility=:publicVisibility ";

        var query = entityManager.createQuery(
                "select c.occurredAt, c.type, c.repository.id, c.repository.visibility from Contribution c " +
                "where c.user.id=:userId and c.repository.includedInAnalysis=true " +
                "and c.occurredAt>=:threshold " + visibilityClause + "order by c.occurredAt",
                Object[].class)
                .setParameter("userId", userId)
                .setParameter("threshold", threshold);
        if (!privacyScope.allowsPrivateAggregates()) {
            query.setParameter("publicVisibility", RepositoryVisibility.PUBLIC);
        }

        List<Object[]> rows = query.getResultList();
        Map<YearMonth, MutableMonth> monthly = new TreeMap<>();
        EnumMap<Contribution.Type, Integer> totals = new EnumMap<>(Contribution.Type.class);
        Set<UUID> activeProjects = new HashSet<>();
        int publicRows = 0;
        int privateRows = 0;
        for (Object[] row : rows) {
            OffsetDateTime occurredAt = (OffsetDateTime) row[0];
            Contribution.Type type = (Contribution.Type) row[1];
            UUID repositoryId = (UUID) row[2];
            RepositoryVisibility visibility = (RepositoryVisibility) row[3];
            totals.merge(type, 1, Integer::sum);
            activeProjects.add(repositoryId);
            if (visibility == RepositoryVisibility.PRIVATE) privateRows++; else publicRows++;
            MutableMonth month = monthly.computeIfAbsent(YearMonth.from(occurredAt), ignored -> new MutableMonth());
            month.contributions++;
            month.projects.add(repositoryId);
        }

        return new ActivityResult(rows.size(), activeProjects.size(), contributionTotals(totals),
                monthly.entrySet().stream().map(entry -> new ActivityMonthResult(
                        entry.getKey().toString(), entry.getValue().contributions, entry.getValue().projects.size()))
                        .toList(),
                privacyProvenance(publicRows, privateRows));
    }

    @Transactional
    public ContributionsResult contributions(UUID userId, ExternalClientToken.PrivacyScope privacyScope) {
        String visibilityClause = privacyScope.allowsPrivateAggregates()
                ? "" : " and c.repository.visibility=:publicVisibility ";
        var query = entityManager.createQuery(
                "select c.type, count(c.id), c.repository.visibility from Contribution c " +
                "where c.user.id=:userId and c.repository.includedInAnalysis=true " + visibilityClause +
                "group by c.type, c.repository.visibility", Object[].class)
                .setParameter("userId", userId);
        if (!privacyScope.allowsPrivateAggregates()) {
            query.setParameter("publicVisibility", RepositoryVisibility.PUBLIC);
        }

        EnumMap<Contribution.Type, Integer> totals = new EnumMap<>(Contribution.Type.class);
        int publicCount = 0;
        int privateCount = 0;
        for (Object[] row : query.getResultList()) {
            Contribution.Type type = (Contribution.Type) row[0];
            int count = ((Number) row[1]).intValue();
            RepositoryVisibility visibility = (RepositoryVisibility) row[2];
            totals.merge(type, count, Integer::sum);
            if (visibility == RepositoryVisibility.PRIVATE) privateCount += count; else publicCount += count;
        }
        return new ContributionsResult(totals.values().stream().mapToInt(Integer::intValue).sum(),
                contributionTotals(totals), privacyProvenance(publicCount, privateCount));
    }

    private List<String> categoryKeys(UUID repositoryId) {
        return entityManager.createQuery(
                "select distinct c.category.categoryKey from RepositoryProjectCategory c " +
                "where c.repository.id=:repositoryId order by c.category.categoryKey", String.class)
                .setParameter("repositoryId", repositoryId)
                .getResultList();
    }

    private List<String> technologyKeys(UUID repositoryId) {
        return entityManager.createQuery(
                "select distinct e.technology.technologyKey from RepositoryTechnologyEvidence e " +
                "where e.repository.id=:repositoryId order by e.technology.technologyKey", String.class)
                .setParameter("repositoryId", repositoryId)
                .getResultList();
    }

    private boolean excludedFromAiProfile(UUID userId, UUID repositoryId) {
        return corrections.exists(userId, repositoryId,
                UserAnalysisCorrection.Type.PROJECT_EXCLUDED_FROM_AI_PROFILE, null);
    }

    private Map<String, Integer> contributionTotals(Map<Contribution.Type, Integer> totals) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Contribution.Type type : Contribution.Type.values()) {
            result.put(type.name().toLowerCase(Locale.ROOT), totals.getOrDefault(type, 0));
        }
        return result;
    }

    private String privacyProvenance(int publicCount, int privateCount) {
        return DataPrivacyProvenance.fromRepositoryCounts(publicCount, privateCount).name();
    }

    private static final class MutableMonth {
        int contributions;
        final Set<UUID> projects = new HashSet<>();
    }

    public record ProjectResult(UUID id, String name, String visibility, String ownership,
                                OffsetDateTime lastActivityAt, List<String> projectTypes,
                                List<String> technologies, boolean excludedFromAiProfile) {}
    public record TechnologySummaryResult(String key, String name, String evidenceLevel,
                                          int evidenceScore, int projectCount,
                                          OffsetDateTime firstObservedAt, OffsetDateTime lastObservedAt,
                                          String privacyProvenance) {}
    public record ProjectTypeSummaryResult(String key, String name, int projectCount) {}
    public record EvidenceResult(List<TechnologyEvidenceResult> technologies,
                                 List<CategoryEvidenceResult> projectTypes) {}
    public record TechnologyEvidenceResult(String technologyKey, String evidenceType,
                                           String strength, int observations,
                                           String privacyProvenance) {}
    public record CategoryEvidenceResult(String projectTypeKey, String source,
                                         String confidence, int observations,
                                         String privacyProvenance) {}
    public record ActivityResult(int contributionCount, int activeProjectCount,
                                 Map<String, Integer> contributionTypes,
                                 List<ActivityMonthResult> monthly, String privacyProvenance) {}
    public record ActivityMonthResult(String month, int contributions, int activeProjects) {}
    public record ContributionsResult(int total, Map<String, Integer> byType, String privacyProvenance) {}
}
