package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
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
        UUID userId = current.user().getId();

        OffsetDateTime fromDate = parseStart(from);
        OffsetDateTime toDate = parseEnd(to);

        StringBuilder jpql = new StringBuilder(
                "select c.occurredAt, c.additions, c.deletions, c.repository.id, c.repository.name " +
                "from Contribution c " +
                "where c.user.id=:userId and c.type=:type"
        );
        if (fromDate != null) jpql.append(" and c.occurredAt >= :fromDate");
        if (toDate != null) jpql.append(" and c.occurredAt < :toDate");
        jpql.append(" order by c.occurredAt");

        var query = entityManager.createQuery(jpql.toString(), Object[].class)
                .setParameter("userId", userId)
                .setParameter("type", io.github.developeranalytics.domain.model.Contribution.Type.COMMIT);
        if (fromDate != null) query.setParameter("fromDate", fromDate);
        if (toDate != null) query.setParameter("toDate", toDate);

        List<Object[]> rows = query.getResultList();
        Map<UUID, String> projectTypes = loadPrimaryProjectTypes(userId);
        Map<UUID, String> technologies = loadPrimaryTechnologies(userId);

        Map<Integer, PeriodAccumulator> years = new TreeMap<>();
        Map<YearMonth, PeriodAccumulator> months = new TreeMap<>();
        Map<LocalDate, PeriodAccumulator> weeks = new TreeMap<>();
        Map<UUID, Map<YearMonth, PeriodAccumulator>> projectMonths = new HashMap<>();
        Map<UUID, Map<LocalDate, PeriodAccumulator>> projectWeeks = new HashMap<>();

        List<Integer> sizes = new ArrayList<>();
        long additions = 0;
        long deletions = 0;
        int lineStatisticsCommitCount = 0;
        OffsetDateTime first = null;
        OffsetDateTime last = null;

        for (Object[] row : rows) {
            OffsetDateTime occurredAt = (OffsetDateTime) row[0];
            Integer add = (Integer) row[1];
            Integer del = (Integer) row[2];
            UUID repositoryId = (UUID) row[3];
            String repositoryName = (String) row[4];
            int a = add == null ? 0 : add;
            int d = del == null ? 0 : del;
            boolean hasLines = add != null || del != null;

            if (hasLines) {
                additions += a;
                deletions += d;
                sizes.add(a + d);
                lineStatisticsCommitCount++;
            }

            accumulate(years.computeIfAbsent(occurredAt.getYear(), ignored -> new PeriodAccumulator()), repositoryId, repositoryName, a, d, hasLines);
            YearMonth month = YearMonth.from(occurredAt);
            accumulate(months.computeIfAbsent(month, ignored -> new PeriodAccumulator()), repositoryId, repositoryName, a, d, hasLines);
            LocalDate week = occurredAt.toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            accumulate(weeks.computeIfAbsent(week, ignored -> new PeriodAccumulator()), repositoryId, repositoryName, a, d, hasLines);
            accumulate(projectMonths.computeIfAbsent(repositoryId, ignored -> new TreeMap<>()).computeIfAbsent(month, ignored -> new PeriodAccumulator()), repositoryId, repositoryName, a, d, hasLines);
            accumulate(projectWeeks.computeIfAbsent(repositoryId, ignored -> new TreeMap<>()).computeIfAbsent(week, ignored -> new PeriodAccumulator()), repositoryId, repositoryName, a, d, hasLines);

            if (first == null || occurredAt.isBefore(first)) first = occurredAt;
            if (last == null || occurredAt.isAfter(last)) last = occurredAt;
        }

        Object[] repositoryStats = entityManager.createQuery(
                "select coalesce(sum(r.userAdditions),0), coalesce(sum(r.userDeletions),0), " +
                "coalesce(sum(r.userCommitCount),0), count(r.id) " +
                "from SourceRepository r where r.user.id=:userId and r.includedInAnalysis=true", Object[].class)
                .setParameter("userId", userId).getSingleResult();
        long measuredAdditions = ((Number) repositoryStats[0]).longValue();
        long measuredDeletions = ((Number) repositoryStats[1]).longValue();
        long measuredCommits = ((Number) repositoryStats[2]).longValue();
        double average = measuredCommits > 0
                ? (double) (measuredAdditions + measuredDeletions) / measuredCommits
                : (sizes.isEmpty() ? 0.0 : sizes.stream().mapToInt(Integer::intValue).average().orElse(0.0));
        if (measuredCommits > 0) {
            additions = measuredAdditions;
            deletions = measuredDeletions;
        }

        List<YearPoint> yearPoints = years.entrySet().stream()
                .map(entry -> toYearPoint(entry.getKey(), entry.getValue()))
                .toList();
        List<MonthPoint> monthPoints = months.entrySet().stream()
                .map(entry -> toMonthPoint(entry.getKey(), entry.getValue()))
                .toList();
        List<WeekPoint> weekPoints = weeks.entrySet().stream()
                .map(entry -> toWeekPoint(entry.getKey(), entry.getValue()))
                .toList();

        int activeProjects = months.values().stream()
                .flatMap(value -> value.projectIds.stream())
                .collect(java.util.stream.Collectors.toSet())
                .size();

        List<Object[]> projectRows = entityManager.createQuery(
                "select c.repository.id, c.repository.name, min(c.occurredAt), max(c.occurredAt), count(c.id) " +
                "from Contribution c where c.user.id=:userId and c.type=:type " +
                "group by c.repository.id, c.repository.name order by min(c.occurredAt)", Object[].class)
                .setParameter("userId", userId)
                .setParameter("type", io.github.developeranalytics.domain.model.Contribution.Type.COMMIT)
                .getResultList();

        List<ProjectLifecycle> projectsOverTime = projectRows.stream().map(row -> {
            UUID repositoryId = (UUID) row[0];
            List<ProjectPeriodActivity> monthlyActivity = projectMonths.getOrDefault(repositoryId, Map.of()).entrySet().stream()
                    .map(entry -> toProjectPeriod(entry.getKey().toString(), entry.getValue()))
                    .toList();
            List<ProjectPeriodActivity> weeklyActivity = projectWeeks.getOrDefault(repositoryId, Map.of()).entrySet().stream()
                    .map(entry -> toProjectPeriod(entry.getKey().toString(), entry.getValue()))
                    .toList();
            return new ProjectLifecycle(
                    repositoryId,
                    (String) row[1],
                    (OffsetDateTime) row[2],
                    (OffsetDateTime) row[3],
                    ((Number) row[4]).intValue(),
                    projectTypes.getOrDefault(repositoryId, "Unclassified"),
                    technologies.getOrDefault(repositoryId, "Unclassified"),
                    monthlyActivity,
                    weeklyActivity
            );
        }).toList();

        return new ActivityResponse(
                rows.size(), activeProjects, average, median(sizes), additions, deletions,
                first, last, yearPoints, monthPoints, weekPoints, projectsOverTime,
                measuredCommits > 0 || lineStatisticsCommitCount > 0,
                lineStatisticsCommitCount
        );
    }

    private void accumulate(PeriodAccumulator accumulator, UUID repositoryId, String repositoryName,
                            int additions, int deletions, boolean hasLines) {
        accumulator.commits++;
        accumulator.projectIds.add(repositoryId);
        accumulator.projectNames.add(repositoryName);
        if (hasLines) {
            accumulator.additions += additions;
            accumulator.deletions += deletions;
            accumulator.lineStatisticsCommitCount++;
        }
    }

    private YearPoint toYearPoint(int year, PeriodAccumulator value) {
        return new YearPoint(year, value.commits, value.additions, value.deletions,
                value.additions + value.deletions, value.lineStatisticsCommitCount,
                value.projectIds.size(), List.copyOf(value.projectNames));
    }

    private MonthPoint toMonthPoint(YearMonth month, PeriodAccumulator value) {
        return new MonthPoint(month.toString(), value.commits, value.additions, value.deletions,
                value.additions + value.deletions, value.lineStatisticsCommitCount,
                value.projectIds.size(), List.copyOf(value.projectNames));
    }

    private WeekPoint toWeekPoint(LocalDate week, PeriodAccumulator value) {
        return new WeekPoint(week.toString(), value.commits, value.additions, value.deletions,
                value.additions + value.deletions, value.lineStatisticsCommitCount,
                value.projectIds.size(), List.copyOf(value.projectNames));
    }

    private ProjectPeriodActivity toProjectPeriod(String period, PeriodAccumulator value) {
        return new ProjectPeriodActivity(period, value.commits, value.additions, value.deletions,
                value.additions + value.deletions, value.lineStatisticsCommitCount);
    }

    private Map<UUID, String> loadPrimaryProjectTypes(UUID userId) {
        List<Object[]> rows = entityManager.createQuery(
                "select c.repository.id, c.category.displayName, c.confidence " +
                "from RepositoryProjectCategory c " +
                "where c.repository.user.id=:userId and c.repository.includedInAnalysis=true",
                Object[].class)
                .setParameter("userId", userId)
                .getResultList();
        Map<UUID, RankedLabel> ranked = new HashMap<>();
        for (Object[] row : rows) {
            UUID repositoryId = (UUID) row[0];
            String label = (String) row[1];
            String confidence = row[2] == null ? "LOW" : row[2].toString();
            int rank = switch (confidence) { case "HIGH" -> 3; case "MEDIUM" -> 2; default -> 1; };
            RankedLabel current = ranked.get(repositoryId);
            if (current == null || rank > current.rank || (rank == current.rank && label.compareToIgnoreCase(current.label) < 0)) {
                ranked.put(repositoryId, new RankedLabel(label, rank));
            }
        }
        Map<UUID, String> result = new HashMap<>();
        ranked.forEach((id, value) -> result.put(id, value.label));
        return result;
    }

    private Map<UUID, String> loadPrimaryTechnologies(UUID userId) {
        List<Object[]> rows = entityManager.createQuery(
                "select e.repository.id, e.technology.displayName, e.strength " +
                "from RepositoryTechnologyEvidence e " +
                "where e.user.id=:userId and e.repository.includedInAnalysis=true",
                Object[].class)
                .setParameter("userId", userId)
                .getResultList();
        Map<UUID, RankedLabel> ranked = new HashMap<>();
        for (Object[] row : rows) {
            UUID repositoryId = (UUID) row[0];
            String label = (String) row[1];
            String strength = row[2] == null ? "EXPOSURE" : row[2].toString();
            int rank = switch (strength) { case "STRONG" -> 4; case "MODERATE" -> 3; case "LIMITED" -> 2; default -> 1; };
            RankedLabel current = ranked.get(repositoryId);
            if (current == null || rank > current.rank || (rank == current.rank && label.compareToIgnoreCase(current.label) < 0)) {
                ranked.put(repositoryId, new RankedLabel(label, rank));
            }
        }
        Map<UUID, String> result = new HashMap<>();
        ranked.forEach((id, value) -> result.put(id, value.label));
        return result;
    }

    private OffsetDateTime parseStart(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
    }

    private OffsetDateTime parseEnd(String value) {
        if (value == null || value.isBlank()) return null;
        return LocalDate.parse(value).plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC);
    }

    private double median(List<Integer> values) {
        if (values.isEmpty()) return 0.0;
        List<Integer> sorted = new ArrayList<>(values);
        sorted.sort(Integer::compareTo);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) return sorted.get(middle);
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private static class PeriodAccumulator {
        int commits;
        long additions;
        long deletions;
        int lineStatisticsCommitCount;
        Set<UUID> projectIds = new HashSet<>();
        Set<String> projectNames = new TreeSet<>();
    }

    private record RankedLabel(String label, int rank) {}

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
            List<WeekPoint> commitsPerWeek,
            List<ProjectLifecycle> projectsOverTime,
            boolean commitSizeStatisticsAvailable,
            int lineStatisticsCommitCount
    ) {}

    public record YearPoint(int year, int commits, long additions, long deletions, long changedLines,
                            int lineStatisticsCommitCount, int activeProjects, List<String> projects) {}
    public record MonthPoint(String month, int commits, long additions, long deletions, long changedLines,
                             int lineStatisticsCommitCount, int activeProjects, List<String> projects) {}
    public record WeekPoint(String week, int commits, long additions, long deletions, long changedLines,
                            int lineStatisticsCommitCount, int activeProjects, List<String> projects) {}
    public record ProjectLifecycle(UUID repositoryId, String repositoryName, OffsetDateTime firstActivityAt,
                                   OffsetDateTime lastActivityAt, int commits, String projectType, String technology,
                                   List<ProjectPeriodActivity> monthlyActivity,
                                   List<ProjectPeriodActivity> weeklyActivity) {}
    public record ProjectPeriodActivity(String period, int commits, long additions, long deletions,
                                        long changedLines, int lineStatisticsCommitCount) {}
}
