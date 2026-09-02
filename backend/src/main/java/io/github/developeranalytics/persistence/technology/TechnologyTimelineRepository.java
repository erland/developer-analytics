package io.github.developeranalytics.persistence.technology;

import io.github.developeranalytics.domain.aggregate.TechnologyActivityMonth;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;

@ApplicationScoped
public class TechnologyTimelineRepository {

    @Inject EntityManager entityManager;

    public void persist(TechnologyActivityMonth month) { entityManager.persist(month); }

    public Optional<TechnologyActivityMonth> findMonth(UUID userId, String technologyKey, LocalDate yearMonth) {
        return entityManager.createQuery(
                "select m from TechnologyActivityMonth m where m.user.id=:userId " +
                "and m.technologyKey=:technologyKey and m.yearMonth=:yearMonth",
                TechnologyActivityMonth.class)
            .setParameter("userId", userId)
            .setParameter("technologyKey", technologyKey)
            .setParameter("yearMonth", yearMonth)
            .getResultStream().findFirst();
    }

    public List<ActivitySourceRow> calculateActivitySource(UUID userId) {
        return entityManager.createQuery(
                "select e.technology.technologyKey, function('date_trunc', 'month', c.occurredAt), " +
                "count(distinct c.repository.id), count(c.id), " +
                "count(distinct case when c.repository.visibility = :publicVisibility then c.repository.id else null end), " +
                "count(distinct case when c.repository.visibility = :privateVisibility then c.repository.id else null end) " +
                "from RepositoryTechnologyEvidence e, Contribution c " +
                "where e.user.id=:userId and e.repository.includedInAnalysis=true " +
                "and c.user.id=:userId and c.repository.id=e.repository.id " +
                "group by e.technology.technologyKey, function('date_trunc', 'month', c.occurredAt)",
                Object[].class)
            .setParameter("userId", userId)
            .setParameter("publicVisibility", RepositoryVisibility.PUBLIC)
            .setParameter("privateVisibility", RepositoryVisibility.PRIVATE)
            .getResultList().stream()
            .map(row -> new ActivitySourceRow(
                    (String) row[0], toLocalDate(row[1]), number(row[2]), number(row[3]), number(row[4]), number(row[5])
            )).toList();
    }

    public List<MetricActivityRow> calculateMetricActivity(UUID userId) {
        List<Object[]> rows = entityManager.createQuery(
                "select e.technology.technologyKey, c.id, c.occurredAt, c.repository.id, c.additions, c.deletions " +
                "from RepositoryTechnologyEvidence e, Contribution c " +
                "where e.user.id=:userId and e.repository.includedInAnalysis=true " +
                "and c.user.id=:userId and c.type=:commitType and c.repository.id=e.repository.id " +
                "order by e.technology.technologyKey, c.occurredAt",
                Object[].class)
            .setParameter("userId", userId)
            .setParameter("commitType", Contribution.Type.COMMIT)
            .getResultList();

        record Key(String technologyKey, YearMonth month) {}
        class Mutable {
            int commits;
            long changedLines;
            int lineStatisticsCommitCount;
            final Set<UUID> repositories = new HashSet<>();
            final Set<UUID> contributions = new HashSet<>();
        }

        Map<Key, Mutable> grouped = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String technologyKey = (String) row[0];
            UUID contributionId = (UUID) row[1];
            OffsetDateTime occurredAt = (OffsetDateTime) row[2];
            UUID repositoryId = (UUID) row[3];
            Integer additions = (Integer) row[4];
            Integer deletions = (Integer) row[5];
            Key key = new Key(technologyKey, YearMonth.from(occurredAt));
            Mutable value = grouped.computeIfAbsent(key, ignored -> new Mutable());
            if (!value.contributions.add(contributionId)) continue;
            value.commits++;
            value.repositories.add(repositoryId);
            if (additions != null || deletions != null) {
                value.changedLines += (additions == null ? 0 : additions) + (deletions == null ? 0 : deletions);
                value.lineStatisticsCommitCount++;
            }
        }

        return grouped.entrySet().stream()
                .map(entry -> new MetricActivityRow(
                        entry.getKey().technologyKey(), entry.getKey().month().toString(),
                        entry.getValue().commits, entry.getValue().changedLines,
                        entry.getValue().lineStatisticsCommitCount, entry.getValue().repositories.size()))
                .toList();
    }

    public List<TechnologyActivityMonth> findMonthsForUser(UUID userId) {
        return entityManager.createQuery(
                "select m from TechnologyActivityMonth m where m.user.id=:userId order by m.technologyKey, m.yearMonth",
                TechnologyActivityMonth.class)
            .setParameter("userId", userId).getResultList();
    }

    public List<EvidenceBoundsRow> evidenceBounds(UUID userId) {
        return entityManager.createQuery(
                "select e.technology.technologyKey, min(e.observedAt), max(e.observedAt), count(distinct e.repository.id) " +
                "from RepositoryTechnologyEvidence e where e.user.id=:userId and e.repository.includedInAnalysis=true " +
                "group by e.technology.technologyKey",
                Object[].class)
            .setParameter("userId", userId).getResultList().stream()
            .map(row -> new EvidenceBoundsRow((String) row[0], (OffsetDateTime) row[1], (OffsetDateTime) row[2], number(row[3])))
            .toList();
    }

    public List<VisibilityRow> visibility(UUID userId) {
        return entityManager.createQuery(
                "select e.technology.technologyKey, r.visibility, count(distinct r.id) " +
                "from RepositoryTechnologyEvidence e join e.repository r " +
                "where e.user.id=:userId and e.repository.includedInAnalysis=true " +
                "group by e.technology.technologyKey, r.visibility",
                Object[].class)
            .setParameter("userId", userId).getResultList().stream()
            .map(row -> new VisibilityRow((String) row[0], (RepositoryVisibility) row[1], number(row[2])))
            .toList();
    }

    private int number(Object value) { return value == null ? 0 : ((Number) value).intValue(); }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime().toLocalDate().withDayOfMonth(1);
        if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime.toLocalDate().withDayOfMonth(1);
        if (value instanceof java.time.LocalDateTime localDateTime) return localDateTime.toLocalDate().withDayOfMonth(1);
        if (value instanceof LocalDate localDate) return localDate.withDayOfMonth(1);
        throw new IllegalStateException("Unsupported month value: " + value);
    }

    public record ActivitySourceRow(String technologyKey, LocalDate yearMonth, int repositoryCount, int activityCount,
                                    int publicRepositoryCount, int privateRepositoryCount) {}
    public record MetricActivityRow(String technologyKey, String month, int commits, long changedLines,
                                    int lineStatisticsCommitCount, int activeProjectCount) {}
    public record EvidenceBoundsRow(String technologyKey, OffsetDateTime firstObservedAt, OffsetDateTime lastObservedAt,
                                    int repositoryCount) {}
    public record VisibilityRow(String technologyKey, RepositoryVisibility visibility, int repositoryCount) {}
}
