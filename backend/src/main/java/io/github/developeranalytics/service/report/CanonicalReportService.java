package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.insight.UserAiInsight;
import io.github.developeranalytics.domain.model.*;
import io.github.developeranalytics.domain.report.CanonicalReport;
import io.github.developeranalytics.domain.technology.UserTechnologyAssessment;
import io.github.developeranalytics.persistence.insight.UserAiInsightRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.persistence.technology.UserTechnologyAssessmentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;

@ApplicationScoped
public class CanonicalReportService {

    @Inject SourceRepositoryRepository repositories;
    @Inject UserTechnologyAssessmentRepository technologies;
    @Inject UserAiInsightRepository aiInsights;
    @Inject EntityManager entityManager;
    @Inject ReportPrivacyPolicy privacyPolicy;

    @Transactional
    public CanonicalReport build(
            UUID userId,
            CanonicalReport.PrivacyScope privacyScope,
            boolean hidePrivateRepositoryNames
    ) {
        Objects.requireNonNull(privacyScope, "privacyScope");

        List<SourceRepository> all = repositories.findAllForUser(userId).stream()
                .filter(SourceRepository::isIncludedInAnalysis)
                .toList();

        List<SourceRepository> publicRepos = all.stream()
                .filter(r -> r.getVisibility() == RepositoryVisibility.PUBLIC)
                .toList();
        List<SourceRepository> privateRepos = all.stream()
                .filter(r -> r.getVisibility() == RepositoryVisibility.PRIVATE)
                .toList();

        Set<UUID> aggregateRepoIds = all.stream()
                .filter(repository ->
                        privacyPolicy.includeInAggregates(
                                repository,
                                privacyScope
                        ))
                .map(SourceRepository::getId)
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new
                ));

        Set<UUID> detailRepoIds = all.stream()
                .filter(repository ->
                        privacyPolicy.includeInProjectDetail(
                                repository,
                                privacyScope
                        ))
                .map(SourceRepository::getId)
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new
                ));

        ActivitySource activity = activity(userId, aggregateRepoIds);
        int includedPrivate = privacyScope == CanonicalReport.PrivacyScope.PUBLIC_ONLY
                ? 0 : privateRepos.size();

        CanonicalReport.DataCoverage coverage =
                new CanonicalReport.DataCoverage(
                        publicRepos.size() + includedPrivate,
                        publicRepos.size(),
                        includedPrivate,
                        detailRepoIds.size(),
                        activity.total()
                );

        String overview = "Analysis covers " + coverage.repositoryCount()
                + " repositories and " + activity.total()
                + " recorded contributions using measured repository evidence"
                + (aiAssessment(userId, privacyScope).available()
                    ? " with a separately labelled AI interpretation."
                    : ".");

        return new CanonicalReport(
                CanonicalReport.MODEL_VERSION,
                OffsetDateTime.now(ZoneOffset.UTC),
                new CanonicalReport.Summary(
                        "Developer Analytics report",
                        overview
                ),
                new CanonicalReport.Period(
                        activity.firstActivityAt(),
                        activity.lastActivityAt()
                ),
                coverage,
                projectCategories(userId, aggregateRepoIds),
                technologyAnalysis(userId, privacyScope),
                new CanonicalReport.Activity(
                        activity.total(),
                        activity.byType(),
                        activity.monthly()
                ),
                significantProjects(
                        userId,
                        detailRepoIds,
                        hidePrivateRepositoryNames
                ),
                aiAssessment(userId, privacyScope),
                new CanonicalReport.Methodology(
                        "Repository, contribution and technology evidence are measured from collected source-control metadata.",
                        "Project classifications, significance scores and AI interpretations are analytical assessments and are not formal statements of proficiency.",
                        "User-controlled AI-profile exclusions affect profile generation without deleting underlying source facts.",
                        List.of(
                                "repository metadata",
                                "contributions",
                                "technology evidence",
                                "project classification",
                                "significance assessment",
                                "optional AI assessment"
                        )
                ),
                privacyScope
        );
    }

    private ActivitySource activity(UUID userId, Set<UUID> repositoryIds) {
        if (repositoryIds.isEmpty()) {
            return new ActivitySource(
                    0, null, null, Map.of(), List.of()
            );
        }

        List<Object[]> rows = entityManager.createQuery(
                "select c.occurredAt, c.type, c.repository.id " +
                "from Contribution c where c.user.id=:userId " +
                "and c.repository.id in :repositoryIds order by c.occurredAt",
                Object[].class
        )
        .setParameter("userId", userId)
        .setParameter("repositoryIds", repositoryIds)
        .getResultList();

        Map<String,Integer> byType = new LinkedHashMap<>();
        for (Contribution.Type type : Contribution.Type.values()) {
            byType.put(type.name(), 0);
        }

        Map<YearMonth, MonthAccumulator> monthly = new TreeMap<>();
        OffsetDateTime first = null;
        OffsetDateTime last = null;

        for (Object[] row : rows) {
            OffsetDateTime at = (OffsetDateTime) row[0];
            Contribution.Type type = (Contribution.Type) row[1];
            UUID repositoryId = (UUID) row[2];

            byType.merge(type.name(), 1, Integer::sum);
            MonthAccumulator month = monthly.computeIfAbsent(
                    YearMonth.from(at),
                    ignored -> new MonthAccumulator()
            );
            month.contributions++;
            month.projects.add(repositoryId);

            if (first == null || at.isBefore(first)) first = at;
            if (last == null || at.isAfter(last)) last = at;
        }

        return new ActivitySource(
                rows.size(),
                first,
                last,
                Map.copyOf(byType),
                monthly.entrySet().stream()
                        .map(entry -> new CanonicalReport.ActivityMonth(
                                entry.getKey().toString(),
                                entry.getValue().contributions,
                                entry.getValue().projects.size()
                        ))
                        .toList()
        );
    }

    private List<CanonicalReport.ProjectCategory> projectCategories(
            UUID userId,
            Set<UUID> repositoryIds
    ) {
        if (repositoryIds.isEmpty()) return List.of();

        return entityManager.createQuery(
                "select c.category.categoryKey, c.category.displayName, " +
                "count(distinct c.repository.id) " +
                "from RepositoryProjectCategory c " +
                "where c.repository.user.id=:userId " +
                "and c.repository.id in :repositoryIds " +
                "group by c.category.categoryKey, c.category.displayName " +
                "order by count(distinct c.repository.id) desc, c.category.displayName",
                Object[].class
        )
        .setParameter("userId", userId)
        .setParameter("repositoryIds", repositoryIds)
        .getResultList()
        .stream()
        .map(row -> new CanonicalReport.ProjectCategory(
                (String) row[0],
                (String) row[1],
                ((Number) row[2]).intValue()
        ))
        .toList();
    }

    private List<CanonicalReport.TechnologyAnalysis> technologyAnalysis(
            UUID userId,
            CanonicalReport.PrivacyScope privacyScope
    ) {
        return technologies.findForUser(userId).stream()
                .filter(a -> privacyScope != CanonicalReport.PrivacyScope.PUBLIC_ONLY
                        || a.getPrivacyProvenance() == DataPrivacyProvenance.PUBLIC_ONLY)
                .map(a -> new CanonicalReport.TechnologyAnalysis(
                        a.getTechnology().getTechnologyKey(),
                        a.getTechnology().getDisplayName(),
                        a.getStrength().name(),
                        a.getScore(),
                        a.getRepositoryCount(),
                        a.getFirstObservedAt(),
                        a.getLastObservedAt(),
                        a.getPrivacyProvenance().name()
                ))
                .toList();
    }

    private List<CanonicalReport.SignificantProject> significantProjects(
            UUID userId,
            Set<UUID> detailRepositoryIds,
            boolean hidePrivateNames
    ) {
        if (detailRepositoryIds.isEmpty()) return List.of();

        List<Object[]> rows = entityManager.createQuery(
                "select a.repository.id, a.repository.name, " +
                "a.repository.visibility, a.repository.ownershipRelation, " +
                "a.significanceLevel, a.significanceScore, " +
                "a.involvementLevel, a.involvementScore " +
                "from ProjectSignificanceAssessment a " +
                "where a.user.id=:userId " +
                "and a.repository.id in :repositoryIds " +
                "and (a.significanceLevel in (:highLevels) " +
                "or a.involvementLevel in (:highLevels)) " +
                "order by a.significanceScore desc, a.involvementScore desc",
                Object[].class
        )
        .setParameter("userId", userId)
        .setParameter("repositoryIds", detailRepositoryIds)
        .setParameter(
                "highLevels",
                List.of(
                        io.github.developeranalytics.domain.project
                                .ProjectSignificanceAssessment.Level.HIGH,
                        io.github.developeranalytics.domain.project
                                .ProjectSignificanceAssessment.Level.VERY_HIGH
                )
        )
        .setMaxResults(15)
        .getResultList();

        int privateIndex = 0;
        List<CanonicalReport.SignificantProject> result =
                new ArrayList<>();

        for (Object[] row : rows) {
            RepositoryVisibility visibility =
                    (RepositoryVisibility) row[2];
            String name = (String) row[1];

            if (visibility == RepositoryVisibility.PRIVATE &&
                    hidePrivateNames) {
                privateIndex++;
                name = "Private repository " + privateIndex;
            }

            result.add(new CanonicalReport.SignificantProject(
                    (UUID) row[0],
                    name,
                    visibility.name(),
                    row[3].toString(),
                    row[4].toString(),
                    ((Number) row[5]).intValue(),
                    row[6].toString(),
                    ((Number) row[7]).intValue()
            ));
        }
        return List.copyOf(result);
    }

    private CanonicalReport.RoleAiAssessment aiAssessment(
            UUID userId,
            CanonicalReport.PrivacyScope privacyScope
    ) {
        Optional<UserAiInsight> latest = aiInsights.latest(userId);
        if (latest.isEmpty()) {
            return CanonicalReport.RoleAiAssessment.unavailable();
        }

        UserAiInsight insight = latest.get();
        if (privacyScope == CanonicalReport.PrivacyScope.PUBLIC_ONLY &&
                insight.getPrivacyProvenance() !=
                        DataPrivacyProvenance.PUBLIC_ONLY) {
            return CanonicalReport.RoleAiAssessment.unavailable();
        }

        return new CanonicalReport.RoleAiAssessment(
                true,
                true,
                insight.getLikelyRoles().stream()
                        .map(role -> new CanonicalReport.Role(
                                role.role(),
                                role.confidence(),
                                role.rationale()
                        ))
                        .toList(),
                insight.getTechnicalFocus(),
                insight.getBreadthDepthObservation(),
                insight.getTechnologyEvolutionSummary(),
                insight.getOpenSourceEngagementSummary(),
                insight.getProviderId(),
                insight.getModelId(),
                insight.getPrivacyProvenance().name()
        );
    }

    private static final class MonthAccumulator {
        int contributions;
        final Set<UUID> projects = new HashSet<>();
    }

    private record ActivitySource(
            int total,
            OffsetDateTime firstActivityAt,
            OffsetDateTime lastActivityAt,
            Map<String,Integer> byType,
            List<CanonicalReport.ActivityMonth> monthly
    ) {}
}
