package io.github.developeranalytics.persistence.project;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
public class ProjectTypeAnalyticsRepository {

    @Inject
    EntityManager entityManager;

    public List<CategorySummaryRow> categorySummaries(UUID userId) {
        return entityManager.createQuery(
                "select c.category.categoryKey, c.category.displayName, " +
                "count(distinct c.repository.id) " +
                "from RepositoryProjectCategory c " +
                "where c.repository.user.id=:userId " +
                "group by c.category.categoryKey, c.category.displayName " +
                "order by count(distinct c.repository.id) desc, c.category.displayName",
                Object[].class)
            .setParameter("userId", userId)
            .getResultList()
            .stream()
            .map(row -> new CategorySummaryRow(
                    (String) row[0],
                    (String) row[1],
                    ((Number) row[2]).intValue()
            ))
            .toList();
    }

    public List<CategoryActivityRow> categoryActivity(UUID userId) {
        List<Object[]> rows = entityManager.createQuery(
                "select c.category.categoryKey, co.occurredAt, co.repository.id " +
                "from RepositoryProjectCategory c, Contribution co " +
                "where c.repository.user.id=:userId " +
                "and co.user.id=:userId " +
                "and co.repository.id=c.repository.id " +
                "order by co.occurredAt",
                Object[].class)
            .setParameter("userId", userId)
            .getResultList();

        Map<Key, MutableActivity> grouped = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String categoryKey = (String) row[0];
            java.time.OffsetDateTime occurredAt =
                    (java.time.OffsetDateTime) row[1];
            UUID repositoryId = (UUID) row[2];

            Key key = new Key(
                    categoryKey,
                    YearMonth.from(occurredAt)
            );

            MutableActivity activity = grouped.computeIfAbsent(
                    key,
                    ignored -> new MutableActivity()
            );
            activity.activityCount++;
            activity.repositories.add(repositoryId);
        }

        return grouped.entrySet()
                .stream()
                .map(entry -> new CategoryActivityRow(
                        entry.getKey().categoryKey,
                        entry.getKey().month.toString(),
                        entry.getValue().activityCount,
                        entry.getValue().repositories.size()
                ))
                .sorted(
                        Comparator.comparing(CategoryActivityRow::categoryKey)
                                .thenComparing(CategoryActivityRow::month)
                )
                .toList();
    }

    public List<RepresentativeProjectRow> representativeProjects(
            UUID userId,
            String categoryKey,
            int limit
    ) {
        return entityManager.createQuery(
                "select c.repository.id, c.repository.name, c.repository.htmlUrl, " +
                "c.repository.visibility, c.repository.ownershipRelation, " +
                "c.repository.lastActivityAt, count(co.id) " +
                "from RepositoryProjectCategory c " +
                "left join Contribution co on co.repository.id=c.repository.id " +
                "and co.user.id=:userId " +
                "where c.repository.user.id=:userId " +
                "and c.category.categoryKey=:categoryKey " +
                "group by c.repository.id, c.repository.name, c.repository.htmlUrl, " +
                "c.repository.visibility, c.repository.ownershipRelation, " +
                "c.repository.lastActivityAt " +
                "order by count(co.id) desc, c.repository.lastActivityAt desc nulls last",
                Object[].class)
            .setParameter("userId", userId)
            .setParameter("categoryKey", categoryKey)
            .setMaxResults(Math.max(1, Math.min(limit, 10)))
            .getResultList()
            .stream()
            .map(row -> new RepresentativeProjectRow(
                    (UUID) row[0],
                    (String) row[1],
                    (String) row[2],
                    row[3].toString(),
                    row[4].toString(),
                    (java.time.OffsetDateTime) row[5],
                    ((Number) row[6]).intValue()
            ))
            .toList();
    }

    private record Key(
            String categoryKey,
            YearMonth month
    ) {}

    private static class MutableActivity {
        int activityCount;
        final Set<UUID> repositories = new HashSet<>();
    }

    public record CategorySummaryRow(
            String categoryKey,
            String categoryName,
            int projectCount
    ) {}

    public record CategoryActivityRow(
            String categoryKey,
            String month,
            int activityCount,
            int activeProjectCount
    ) {}

    public record RepresentativeProjectRow(
            UUID repositoryId,
            String repositoryName,
            String htmlUrl,
            String visibility,
            String ownershipRelation,
            java.time.OffsetDateTime lastActivityAt,
            int contributionCount
    ) {}
}
