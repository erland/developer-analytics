package io.github.developeranalytics.service.external;

import io.github.developeranalytics.domain.correction.UserAnalysisCorrection;
import io.github.developeranalytics.domain.external.ExternalClientToken;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.DataPrivacyProvenance;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.correction.UserAnalysisCorrectionRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
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
                        categoryKeys(userId, repository.getId()), technologyKeys(userId, repository.getId()),
                        excludedFromAiProfile(userId, repository.getId())))
                .toList();
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

    private List<String> categoryKeys(UUID userId, UUID repositoryId) {
        return entityManager.createQuery(
                "select distinct c.category.categoryKey from RepositoryProjectCategory c " +
                "where c.repository.id=:repositoryId and not exists (" +
                "select 1 from UserAnalysisCorrection correction where correction.user.id=:userId " +
                "and correction.repository.id=:repositoryId " +
                "and correction.type=io.github.developeranalytics.domain.correction.UserAnalysisCorrection.Type.PROJECT_CATEGORY_REJECTED " +
                "and correction.correctionKey=c.category.categoryKey)", String.class)
                .setParameter("repositoryId", repositoryId)
                .setParameter("userId", userId)
                .getResultList();
    }

    private List<String> technologyKeys(UUID userId, UUID repositoryId) {
        return entityManager.createQuery(
                "select distinct e.technology.technologyKey from RepositoryTechnologyEvidence e " +
                "where e.repository.id=:repositoryId order by e.technology.technologyKey", String.class)
                .setParameter("repositoryId", repositoryId)
                .getResultList().stream()
                .filter(key -> !isTechnologySuppressed(userId, key))
                .toList();
    }

    private boolean isTechnologySuppressed(UUID userId, String key) {
        return corrections.exists(userId, null,
                UserAnalysisCorrection.Type.TECHNOLOGY_INFERENCE_SUPPRESSED, key);
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
    public record ActivityResult(int contributionCount, int activeProjectCount,
                                 Map<String, Integer> contributionTypes,
                                 List<ActivityMonthResult> monthly, String privacyProvenance) {}
    public record ActivityMonthResult(String month, int contributions, int activeProjects) {}
    public record ContributionsResult(int total, Map<String, Integer> byType, String privacyProvenance) {}
}
