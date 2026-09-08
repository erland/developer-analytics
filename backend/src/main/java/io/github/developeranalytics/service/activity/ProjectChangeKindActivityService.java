package io.github.developeranalytics.service.activity;

import io.github.developeranalytics.domain.change.ChangeKind;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;

@ApplicationScoped
public class ProjectChangeKindActivityService {
    @Inject EntityManager entityManager;

    @Transactional
    public Result get(UUID userId, UUID repositoryId, Set<ChangeKind> changeKinds) {
        List<Object[]> rows = entityManager.createQuery(
                "select f.contribution.id, f.occurredAt, sum(f.additions), sum(f.deletions) " +
                        "from ContributionFileChange f where f.user.id=:userId and f.repository.id=:repositoryId " +
                        "and f.changeKind in :changeKinds group by f.contribution.id, f.occurredAt order by f.occurredAt",
                Object[].class)
                .setParameter("userId", userId)
                .setParameter("repositoryId", repositoryId)
                .setParameter("changeKinds", changeKinds)
                .getResultList();

        Map<YearMonth, Month> months = new TreeMap<>();
        long additions = 0;
        long deletions = 0;
        OffsetDateTime first = null;
        OffsetDateTime last = null;
        for (Object[] row : rows) {
            OffsetDateTime at = (OffsetDateTime) row[1];
            long add = ((Number) row[2]).longValue();
            long del = ((Number) row[3]).longValue();
            additions += add;
            deletions += del;
            if (first == null || at.isBefore(first)) first = at;
            if (last == null || at.isAfter(last)) last = at;
            Month month = months.computeIfAbsent(YearMonth.from(at), ignored -> new Month());
            month.commits++;
            month.additions += add;
            month.deletions += del;
        }
        List<Point> timeline = months.entrySet().stream()
                .map(entry -> new Point(entry.getKey().toString(), entry.getValue().commits,
                        entry.getValue().additions, entry.getValue().deletions,
                        entry.getValue().additions + entry.getValue().deletions,
                        entry.getValue().commits))
                .toList();
        return new Result(rows.size(), additions, deletions, first, last, timeline);
    }

    private static final class Month { int commits; long additions; long deletions; }
    public record Result(int commits, long additions, long deletions,
                         OffsetDateTime firstActivityAt, OffsetDateTime lastActivityAt, List<Point> timeline) {}
    public record Point(String month, int commits, long additions, long deletions,
                        long changedLines, int lineStatisticsCommitCount) {}
}
