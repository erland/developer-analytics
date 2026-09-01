package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;

@Path("/api/me/activity")
@Produces(MediaType.APPLICATION_JSON)
public class MeActivityResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    EntityManager entityManager;

    @GET
    @Transactional
    public ActivityResponse get(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @QueryParam("from") String from,
            @QueryParam("to") String to
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);

        OffsetDateTime fromDate = parseStart(from);
        OffsetDateTime toDate = parseEnd(to);

        StringBuilder jpql = new StringBuilder(
                "select c.occurredAt, c.additions, c.deletions, c.repository.id, c.repository.name " +
                "from Contribution c " +
                "where c.user.id=:userId and c.type=:type"
        );
        if (fromDate != null) {
            jpql.append(" and c.occurredAt >= :fromDate");
        }
        if (toDate != null) {
            jpql.append(" and c.occurredAt < :toDate");
        }
        jpql.append(" order by c.occurredAt");

        var query = entityManager.createQuery(jpql.toString(), Object[].class)
            .setParameter("userId", current.user().getId())
            .setParameter("type", io.github.developeranalytics.domain.model.Contribution.Type.COMMIT);

        if (fromDate != null) {
            query.setParameter("fromDate", fromDate);
        }
        if (toDate != null) {
            query.setParameter("toDate", toDate);
        }

        List<Object[]> rows = query.getResultList();

        Map<Integer, Integer> commitsPerYear = new TreeMap<>();
        Map<YearMonth, Integer> commitsPerMonth = new TreeMap<>();
        Map<YearMonth, Set<UUID>> activeProjectsPerMonth = new TreeMap<>();
        Map<YearMonth, Set<String>> projectNamesPerMonth = new TreeMap<>();
        Map<Integer, Set<String>> projectNamesPerYear = new TreeMap<>();
        Map<UUID, Map<YearMonth, Integer>> projectMonthlyActivity = new HashMap<>();

        List<Integer> sizes = new ArrayList<>();
        long additions = 0;
        long deletions = 0;

        OffsetDateTime first = null;
        OffsetDateTime last = null;

        for (Object[] row : rows) {
            OffsetDateTime occurredAt = (OffsetDateTime) row[0];
            Integer add = (Integer) row[1];
            Integer del = (Integer) row[2];
            UUID repositoryId = (UUID) row[3];
            String repositoryName = (String) row[4];

            commitsPerYear.merge(occurredAt.getYear(), 1, Integer::sum);
            projectNamesPerYear.computeIfAbsent(occurredAt.getYear(), ignored -> new TreeSet<>()).add(repositoryName);
            YearMonth month = YearMonth.from(occurredAt);
            commitsPerMonth.merge(month, 1, Integer::sum);
            activeProjectsPerMonth
                    .computeIfAbsent(month, ignored -> new HashSet<>())
                    .add(repositoryId);
            projectNamesPerMonth.computeIfAbsent(month, ignored -> new TreeSet<>()).add(repositoryName);
            projectMonthlyActivity.computeIfAbsent(repositoryId, ignored -> new TreeMap<>()).merge(month, 1, Integer::sum);

            if (add != null || del != null) {
                int a = add == null ? 0 : add;
                int d = del == null ? 0 : del;
                additions += a;
                deletions += d;
                sizes.add(a + d);
            }

            if (first == null || occurredAt.isBefore(first)) first = occurredAt;
            if (last == null || occurredAt.isAfter(last)) last = occurredAt;
        }

        Object[] repositoryStats = entityManager.createQuery(
                "select coalesce(sum(r.userAdditions),0), coalesce(sum(r.userDeletions),0), " +
                "coalesce(sum(r.userCommitCount),0), count(r.id) " +
                "from SourceRepository r where r.user.id=:userId and r.includedInAnalysis=true", Object[].class)
                .setParameter("userId", current.user().getId()).getSingleResult();
        long measuredAdditions = ((Number) repositoryStats[0]).longValue();
        long measuredDeletions = ((Number) repositoryStats[1]).longValue();
        long measuredCommits = ((Number) repositoryStats[2]).longValue();
        double average = measuredCommits > 0 ? (double) (measuredAdditions + measuredDeletions) / measuredCommits :
                (sizes.isEmpty() ? 0.0 : sizes.stream().mapToInt(Integer::intValue).average().orElse(0.0));
        if (measuredCommits > 0) { additions = measuredAdditions; deletions = measuredDeletions; }
        double median = median(sizes);

        List<YearPoint> years = commitsPerYear.entrySet().stream()
                .map(entry -> new YearPoint(entry.getKey(), entry.getValue(),
                        projectNamesPerYear.getOrDefault(entry.getKey(), Set.of()).size(),
                        List.copyOf(projectNamesPerYear.getOrDefault(entry.getKey(), Set.of()))))
                .toList();

        List<MonthPoint> months = commitsPerMonth.entrySet().stream()
                .map(entry -> new MonthPoint(
                        entry.getKey().toString(),
                        entry.getValue(),
                        activeProjectsPerMonth.getOrDefault(entry.getKey(), Set.of()).size(),
                        List.copyOf(projectNamesPerMonth.getOrDefault(entry.getKey(), Set.of()))
                ))
                .toList();

        int activeProjects = activeProjectsPerMonth.values().stream()
                .flatMap(Set::stream)
                .collect(java.util.stream.Collectors.toSet())
                .size();

        List<Object[]> projectRows = entityManager.createQuery(
                "select c.repository.id, c.repository.name, min(c.occurredAt), max(c.occurredAt), count(c.id) " +
                "from Contribution c where c.user.id=:userId and c.type=:type " +
                "group by c.repository.id, c.repository.name order by min(c.occurredAt)", Object[].class)
                .setParameter("userId", current.user().getId())
                .setParameter("type", io.github.developeranalytics.domain.model.Contribution.Type.COMMIT)
                .getResultList();
        List<ProjectLifecycle> projectsOverTime = projectRows.stream().map(row -> {
            UUID repositoryId = (UUID) row[0];
            List<ProjectMonthActivity> activity = projectMonthlyActivity.getOrDefault(repositoryId, Map.of()).entrySet().stream()
                    .map(e -> new ProjectMonthActivity(e.getKey().toString(), e.getValue())).toList();
            return new ProjectLifecycle(repositoryId, (String) row[1], (OffsetDateTime) row[2], (OffsetDateTime) row[3],
                    ((Number) row[4]).intValue(), activity);
        }).toList();

        return new ActivityResponse(
                rows.size(),
                activeProjects,
                average,
                median,
                additions,
                deletions,
                first,
                last,
                years,
                months,
                projectsOverTime,
                measuredCommits > 0
        );
    }

    private OffsetDateTime parseStart(String value) {
        if (value == null || value.isBlank()) return null;
        return java.time.LocalDate.parse(value).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
    }

    private OffsetDateTime parseEnd(String value) {
        if (value == null || value.isBlank()) return null;
        return java.time.LocalDate.parse(value).plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
    }

    private double median(List<Integer> values) {
        if (values.isEmpty()) return 0.0;
        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(Integer::compareTo);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) return sorted.get(middle);
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    public record ActivityResponse(
            int commitCount,
            int activeProjects,
            double averageCommitSize,
            double medianCommitSize,
            long additions,
            long deletions,
            OffsetDateTime firstActivityAt,
            OffsetDateTime lastActivityAt,
            List<YearPoint> commitsPerYear,
            List<MonthPoint> commitsPerMonth,
            List<ProjectLifecycle> projectsOverTime,
            boolean commitSizeStatisticsAvailable
    ) {}

    public record YearPoint(int year, int commits, int activeProjects, List<String> projects) {}
    public record MonthPoint(String month, int commits, int activeProjects, List<String> projects) {}
    public record ProjectLifecycle(UUID repositoryId, String repositoryName, OffsetDateTime firstActivityAt,
                                   OffsetDateTime lastActivityAt, int commits, List<ProjectMonthActivity> monthlyActivity) {}
    public record ProjectMonthActivity(String month, int commits) {}
}
