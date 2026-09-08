package io.github.developeranalytics.service.report;

import io.github.developeranalytics.domain.change.ChangeKind;
import io.github.developeranalytics.domain.insight.UserAiInsight;
import io.github.developeranalytics.domain.model.*;
import io.github.developeranalytics.domain.report.CanonicalReport;
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
    public CanonicalReport build(UUID userId, CanonicalReport.PrivacyScope privacyScope, boolean hidePrivateRepositoryNames) {
        return build(userId, privacyScope, hidePrivateRepositoryNames, Set.of());
    }

    @Transactional
    public CanonicalReport build(
            UUID userId,
            CanonicalReport.PrivacyScope privacyScope,
            boolean hidePrivateRepositoryNames,
            Set<ChangeKind> changeKinds
    ) {
        Objects.requireNonNull(privacyScope, "privacyScope");
        Set<ChangeKind> effectiveKinds = changeKinds == null ? Set.of() : changeKinds;

        List<SourceRepository> all = repositories.findAllForUser(userId).stream()
                .filter(SourceRepository::isIncludedInAnalysis)
                .toList();
        List<SourceRepository> publicRepos = all.stream()
                .filter(r -> r.getVisibility() == RepositoryVisibility.PUBLIC).toList();
        List<SourceRepository> privateRepos = all.stream()
                .filter(r -> r.getVisibility() == RepositoryVisibility.PRIVATE).toList();

        Set<UUID> aggregateRepoIds = all.stream()
                .filter(repository -> privacyPolicy.includeInAggregates(repository, privacyScope))
                .map(SourceRepository::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Set<UUID> detailRepoIds = all.stream()
                .filter(repository -> privacyPolicy.includeInProjectDetail(repository, privacyScope))
                .map(SourceRepository::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));

        ActivitySource activity = effectiveKinds.isEmpty()
                ? activity(userId, aggregateRepoIds)
                : filteredActivity(userId, aggregateRepoIds, effectiveKinds);
        int includedPrivate = privacyScope == CanonicalReport.PrivacyScope.PUBLIC_ONLY ? 0 : privateRepos.size();

        CanonicalReport.DataCoverage coverage = new CanonicalReport.DataCoverage(
                publicRepos.size() + includedPrivate,
                publicRepos.size(),
                includedPrivate,
                detailRepoIds.size(),
                activity.total());

        CanonicalReport.ChangeScope changeScope = new CanonicalReport.ChangeScope(
                effectiveKinds.isEmpty(),
                effectiveKinds.stream().map(Enum::name).sorted().toList());
        String changeScopeDescription = effectiveKinds.isEmpty()
                ? "all recorded contribution activity"
                : "commit activity touching " + String.join(", ", changeScope.changeKinds());

        CanonicalReport.RoleAiAssessment aiAssessment = aiAssessment(userId, privacyScope);
        String overview = "Analysis covers " + coverage.repositoryCount()
                + " repositories and " + activity.total() + " contributions in " + changeScopeDescription
                + " using measured repository evidence"
                + (aiAssessment.available() ? " with a separately labelled AI interpretation." : ".");

        return new CanonicalReport(
                CanonicalReport.MODEL_VERSION,
                OffsetDateTime.now(ZoneOffset.UTC),
                new CanonicalReport.Summary("Developer Analytics report", overview),
                new CanonicalReport.Period(activity.firstActivityAt(), activity.lastActivityAt()),
                coverage,
                changeScope,
                projectCategories(userId, aggregateRepoIds),
                technologyAnalysis(userId, privacyScope),
                new CanonicalReport.Activity(activity.total(), activity.byType(), activity.monthly()),
                significantProjects(userId, detailRepoIds, hidePrivateRepositoryNames),
                aiAssessment,
                new CanonicalReport.Methodology(
                        "Repository, contribution and technology evidence are measured from collected source-control metadata.",
                        "Project classifications, significance scores and AI interpretations are analytical assessments and are not formal statements of proficiency.",
                        "User-controlled AI-profile exclusions affect profile generation without deleting underlying source facts.",
                        effectiveKinds.isEmpty()
                                ? "Activity scope includes all recorded contribution types."
                                : "Activity scope is derived from changed-file classification; commits are counted once when they touch any selected change kind, and non-commit contribution types are excluded because they do not have changed-file classifications.",
                        List.of("repository metadata", "contributions", "changed-file classifications", "technology evidence", "project classification", "significance assessment", "optional AI assessment")),
                privacyScope);
    }

    private ActivitySource activity(UUID userId, Set<UUID> repositoryIds) {
        if (repositoryIds.isEmpty()) return new ActivitySource(0, null, null, Map.of(), List.of());
        List<Object[]> rows = entityManager.createQuery(
                "select c.occurredAt, c.type, c.repository.id from Contribution c " +
                "where c.user.id=:userId and c.repository.id in :repositoryIds order by c.occurredAt",
                Object[].class)
                .setParameter("userId", userId)
                .setParameter("repositoryIds", repositoryIds)
                .getResultList();
        return aggregateContributionRows(rows);
    }

    private ActivitySource filteredActivity(UUID userId, Set<UUID> repositoryIds, Set<ChangeKind> changeKinds) {
        if (repositoryIds.isEmpty()) return new ActivitySource(0, null, null, Map.of(), List.of());
        List<Object[]> rows = entityManager.createQuery(
                "select f.occurredAt, f.contribution.id, f.repository.id from ContributionFileChange f " +
                "where f.user.id=:userId and f.repository.id in :repositoryIds and f.changeKind in :changeKinds " +
                "order by f.occurredAt",
                Object[].class)
                .setParameter("userId", userId)
                .setParameter("repositoryIds", repositoryIds)
                .setParameter("changeKinds", changeKinds)
                .getResultList();

        Map<String,Integer> byType = zeroContributionTotals();
        Map<YearMonth, MonthAccumulator> monthly = new TreeMap<>();
        Set<UUID> seenContributions = new HashSet<>();
        OffsetDateTime first = null;
        OffsetDateTime last = null;

        for (Object[] row : rows) {
            OffsetDateTime at = (OffsetDateTime) row[0];
            UUID contributionId = (UUID) row[1];
            UUID repositoryId = (UUID) row[2];
            if (!seenContributions.add(contributionId)) continue;
            byType.merge(Contribution.Type.COMMIT.name(), 1, Integer::sum);
            MonthAccumulator month = monthly.computeIfAbsent(YearMonth.from(at), ignored -> new MonthAccumulator());
            month.contributions++;
            month.projects.add(repositoryId);
            if (first == null || at.isBefore(first)) first = at;
            if (last == null || at.isAfter(last)) last = at;
        }
        return activitySource(seenContributions.size(), first, last, byType, monthly);
    }

    private ActivitySource aggregateContributionRows(List<Object[]> rows) {
        Map<String,Integer> byType = zeroContributionTotals();
        Map<YearMonth, MonthAccumulator> monthly = new TreeMap<>();
        OffsetDateTime first = null;
        OffsetDateTime last = null;
        for (Object[] row : rows) {
            OffsetDateTime at = (OffsetDateTime) row[0];
            Contribution.Type type = (Contribution.Type) row[1];
            UUID repositoryId = (UUID) row[2];
            byType.merge(type.name(), 1, Integer::sum);
            MonthAccumulator month = monthly.computeIfAbsent(YearMonth.from(at), ignored -> new MonthAccumulator());
            month.contributions++;
            month.projects.add(repositoryId);
            if (first == null || at.isBefore(first)) first = at;
            if (last == null || at.isAfter(last)) last = at;
        }
        return activitySource(rows.size(), first, last, byType, monthly);
    }

    private Map<String,Integer> zeroContributionTotals() {
        Map<String,Integer> result = new LinkedHashMap<>();
        for (Contribution.Type type : Contribution.Type.values()) result.put(type.name(), 0);
        return result;
    }

    private ActivitySource activitySource(int total, OffsetDateTime first, OffsetDateTime last,
                                          Map<String,Integer> byType, Map<YearMonth, MonthAccumulator> monthly) {
        return new ActivitySource(total, first, last, Map.copyOf(byType), monthly.entrySet().stream()
                .map(entry -> new CanonicalReport.ActivityMonth(entry.getKey().toString(),
                        entry.getValue().contributions, entry.getValue().projects.size()))
                .toList());
    }

    private List<CanonicalReport.ProjectCategory> projectCategories(UUID userId, Set<UUID> repositoryIds) {
        if (repositoryIds.isEmpty()) return List.of();
        return entityManager.createQuery(
                "select c.category.categoryKey, c.category.displayName, count(distinct c.repository.id) " +
                "from RepositoryProjectCategory c where c.repository.user.id=:userId and c.repository.id in :repositoryIds " +
                "group by c.category.categoryKey, c.category.displayName " +
                "order by count(distinct c.repository.id) desc, c.category.displayName",
                Object[].class)
                .setParameter("userId", userId)
                .setParameter("repositoryIds", repositoryIds)
                .getResultList().stream()
                .map(row -> new CanonicalReport.ProjectCategory((String) row[0], (String) row[1], ((Number) row[2]).intValue()))
                .toList();
    }

    private List<CanonicalReport.TechnologyAnalysis> technologyAnalysis(UUID userId, CanonicalReport.PrivacyScope privacyScope) {
        return technologies.findForUser(userId).stream()
                .filter(a -> privacyScope != CanonicalReport.PrivacyScope.PUBLIC_ONLY
                        || a.getPrivacyProvenance() == DataPrivacyProvenance.PUBLIC_ONLY)
                .map(a -> new CanonicalReport.TechnologyAnalysis(
                        a.getTechnology().getTechnologyKey(), a.getTechnology().getDisplayName(),
                        a.getStrength().name(), a.getScore(), a.getRepositoryCount(), a.getFirstObservedAt(),
                        a.getLastObservedAt(), a.getPrivacyProvenance().name()))
                .toList();
    }

    private List<CanonicalReport.SignificantProject> significantProjects(UUID userId, Set<UUID> detailRepositoryIds,
                                                                         boolean hidePrivateNames) {
        if (detailRepositoryIds.isEmpty()) return List.of();
        List<Object[]> rows = entityManager.createQuery(
                "select a.repository.id, a.repository.name, a.repository.visibility, a.repository.ownershipRelation, " +
                "a.significanceLevel, a.significanceScore, a.involvementLevel, a.involvementScore " +
                "from ProjectSignificanceAssessment a where a.user.id=:userId and a.repository.id in :repositoryIds " +
                "and (a.significanceLevel in (:highLevels) or a.involvementLevel in (:highLevels)) " +
                "order by a.significanceScore desc, a.involvementScore desc",
                Object[].class)
                .setParameter("userId", userId)
                .setParameter("repositoryIds", detailRepositoryIds)
                .setParameter("highLevels", List.of(
                        io.github.developeranalytics.domain.project.ProjectSignificanceAssessment.Level.HIGH,
                        io.github.developeranalytics.domain.project.ProjectSignificanceAssessment.Level.VERY_HIGH))
                .setMaxResults(15).getResultList();
        int privateIndex = 0;
        List<CanonicalReport.SignificantProject> result = new ArrayList<>();
        for (Object[] row : rows) {
            RepositoryVisibility visibility = (RepositoryVisibility) row[2];
            String name = (String) row[1];
            if (visibility == RepositoryVisibility.PRIVATE && hidePrivateNames) name = "Private repository " + (++privateIndex);
            result.add(new CanonicalReport.SignificantProject((UUID) row[0], name, visibility.name(), row[3].toString(),
                    row[4].toString(), ((Number) row[5]).intValue(), row[6].toString(), ((Number) row[7]).intValue()));
        }
        return List.copyOf(result);
    }

    private CanonicalReport.RoleAiAssessment aiAssessment(UUID userId, CanonicalReport.PrivacyScope privacyScope) {
        Optional<UserAiInsight> latest = aiInsights.latest(userId);
        if (latest.isEmpty()) return CanonicalReport.RoleAiAssessment.unavailable();
        UserAiInsight insight = latest.get();
        if (privacyScope == CanonicalReport.PrivacyScope.PUBLIC_ONLY
                && insight.getPrivacyProvenance() != DataPrivacyProvenance.PUBLIC_ONLY) {
            return CanonicalReport.RoleAiAssessment.unavailable();
        }
        return new CanonicalReport.RoleAiAssessment(true, true,
                insight.getLikelyRoles().stream().map(role -> new CanonicalReport.Role(
                        role.role(), role.confidence(), role.rationale())).toList(),
                insight.getTechnicalFocus(), insight.getBreadthDepthObservation(),
                insight.getTechnologyEvolutionSummary(), insight.getOpenSourceEngagementSummary(),
                insight.getProviderId(), insight.getModelId(), insight.getPrivacyProvenance().name());
    }

    private static final class MonthAccumulator {
        int contributions;
        final Set<UUID> projects = new HashSet<>();
    }

    private record ActivitySource(int total, OffsetDateTime firstActivityAt, OffsetDateTime lastActivityAt,
                                  Map<String,Integer> byType, List<CanonicalReport.ActivityMonth> monthly) {}
}
