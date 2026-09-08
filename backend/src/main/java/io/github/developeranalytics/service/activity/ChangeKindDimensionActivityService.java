package io.github.developeranalytics.service.activity;

import io.github.developeranalytics.domain.change.ChangeKind;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.YearMonth;
import java.util.*;

/** Change-kind-aware monthly activity for project-type and technology dimensions. */
@ApplicationScoped
public class ChangeKindDimensionActivityService {
    @Inject EntityManager entityManager;

    @Transactional
    public Map<String, List<MetricRow>> categoryActivity(UUID userId, Set<ChangeKind> kinds) {
        List<Object[]> rows = entityManager.createQuery(
                "select distinct f.id, f.contribution.id, f.occurredAt, f.repository.id, " +
                        "c.category.categoryKey, f.additions, f.deletions " +
                        "from ContributionFileChange f, RepositoryProjectCategory c " +
                        "where f.user.id=:userId and c.repository.id=f.repository.id " +
                        "and c.repository.user.id=:userId and c.repository.includedInAnalysis=true " +
                        "and f.changeKind in :changeKinds",
                Object[].class)
                .setParameter("userId", userId)
                .setParameter("changeKinds", kinds)
                .getResultList();
        return aggregate(rows);
    }

    @Transactional
    public Map<String, List<MetricRow>> technologyActivity(UUID userId, Set<ChangeKind> kinds) {
        List<Object[]> rows = entityManager.createQuery(
                "select distinct f.id, f.contribution.id, f.occurredAt, f.repository.id, " +
                        "e.technology.technologyKey, f.additions, f.deletions " +
                        "from ContributionFileChange f, RepositoryTechnologyEvidence e " +
                        "where f.user.id=:userId and e.repository.id=f.repository.id and e.user.id=:userId " +
                        "and e.repository.includedInAnalysis=true and f.changeKind in :changeKinds",
                Object[].class)
                .setParameter("userId", userId)
                .setParameter("changeKinds", kinds)
                .getResultList();
        return aggregate(rows);
    }

    private Map<String, List<MetricRow>> aggregate(List<Object[]> rows) {
        Map<DimensionMonth, MonthAccumulator> months = new HashMap<>();
        for (Object[] row : rows) {
            UUID contributionId = (UUID) row[1];
            YearMonth month = YearMonth.from((java.time.OffsetDateTime) row[2]);
            UUID repositoryId = (UUID) row[3];
            String dimensionKey = (String) row[4];
            long additions = ((Number) row[5]).longValue();
            long deletions = ((Number) row[6]).longValue();
            MonthAccumulator accumulator = months.computeIfAbsent(
                    new DimensionMonth(dimensionKey, month), ignored -> new MonthAccumulator());
            accumulator.contributions.add(contributionId);
            accumulator.repositories.add(repositoryId);
            accumulator.changedLines += additions + deletions;
        }

        Map<String, List<MetricRow>> result = new HashMap<>();
        months.forEach((key, value) -> result.computeIfAbsent(key.dimensionKey, ignored -> new ArrayList<>())
                .add(new MetricRow(key.month.toString(), value.contributions.size(), value.changedLines,
                        value.contributions.size(), value.repositories.size())));
        result.values().forEach(values -> values.sort(Comparator.comparing(MetricRow::month)));
        result.replaceAll((key, values) -> List.copyOf(values));
        return result;
    }

    private record DimensionMonth(String dimensionKey, YearMonth month) {}
    private static final class MonthAccumulator {
        final Set<UUID> contributions = new HashSet<>();
        final Set<UUID> repositories = new HashSet<>();
        long changedLines;
    }

    public record MetricRow(String month, int commits, long changedLines,
                            int lineStatisticsCommitCount, int activeProjectCount) {}
}
