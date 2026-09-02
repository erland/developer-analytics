package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.persistence.repository.RepositoryUserActivityWeekRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;

@Path("/api/me/activity")
@Produces(MediaType.APPLICATION_JSON)
public class MeActivityResource {
    @Inject CurrentUserService currentUserService;
    @Inject EntityManager entityManager;
    @Inject RepositoryUserActivityWeekRepository weeklyActivity;

    @GET
    @Transactional
    public ActivityResponse get(@CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
                                @QueryParam("from") String from, @QueryParam("to") String to) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        UUID userId = current.user().getId();
        OffsetDateTime fromDate = parseStart(from);
        OffsetDateTime toDate = parseEnd(to);

        StringBuilder jpql = new StringBuilder(
                "select c.occurredAt, c.repository.id, c.repository.name from Contribution c " +
                "where c.user.id=:userId and c.type=:type");
        if (fromDate != null) jpql.append(" and c.occurredAt >= :fromDate");
        if (toDate != null) jpql.append(" and c.occurredAt < :toDate");
        jpql.append(" order by c.occurredAt");
        var query = entityManager.createQuery(jpql.toString(), Object[].class)
                .setParameter("userId", userId).setParameter("type", Contribution.Type.COMMIT);
        if (fromDate != null) query.setParameter("fromDate", fromDate);
        if (toDate != null) query.setParameter("toDate", toDate);
        List<Object[]> rows = query.getResultList();

        Map<UUID, String> projectTypes = loadPrimaryProjectTypes(userId);
        Map<UUID, String> technologies = loadPrimaryTechnologies(userId);
        Map<UUID, String> repositoryNames = new HashMap<>();

        Map<Integer, PeriodAccumulator> years = new TreeMap<>();
        Map<YearMonth, PeriodAccumulator> months = new TreeMap<>();
        Map<LocalDate, PeriodAccumulator> weeks = new TreeMap<>();
        Map<UUID, Map<YearMonth, PeriodAccumulator>> projectMonths = new HashMap<>();
        Map<UUID, Map<YearMonth, Map<LocalDate, PeriodAccumulator>>> projectMonthWeeks = new HashMap<>();

        OffsetDateTime first = null;
        OffsetDateTime last = null;
        for (Object[] row : rows) {
            OffsetDateTime occurredAt = (OffsetDateTime) row[0];
            UUID repositoryId = (UUID) row[1];
            String repositoryName = (String) row[2];
            repositoryNames.put(repositoryId, repositoryName);
            YearMonth month = YearMonth.from(occurredAt);
            LocalDate week = occurredAt.toLocalDate().minusDays(occurredAt.getDayOfWeek().getValue() % 7L);
            accumulateCommit(years.computeIfAbsent(occurredAt.getYear(), ignored -> new PeriodAccumulator()), repositoryId, repositoryName);
            accumulateCommit(months.computeIfAbsent(month, ignored -> new PeriodAccumulator()), repositoryId, repositoryName);
            accumulateCommit(weeks.computeIfAbsent(week, ignored -> new PeriodAccumulator()), repositoryId, repositoryName);
            accumulateCommit(projectMonths.computeIfAbsent(repositoryId, ignored -> new TreeMap<>())
                    .computeIfAbsent(month, ignored -> new PeriodAccumulator()), repositoryId, repositoryName);
            accumulateCommit(projectMonthWeeks.computeIfAbsent(repositoryId, ignored -> new TreeMap<>())
                    .computeIfAbsent(month, ignored -> new TreeMap<>())
                    .computeIfAbsent(week, ignored -> new PeriodAccumulator()), repositoryId, repositoryName);
            if (first == null || occurredAt.isBefore(first)) first = occurredAt;
            if (last == null || occurredAt.isAfter(last)) last = occurredAt;
        }

        int lineStatisticsCommitCount = 0;
        for (RepositoryUserActivityWeekRepository.WeekRow row : weeklyActivity.findForUser(userId)) {
            LocalDate week = row.weekStart();
            if (fromDate != null && week.isBefore(fromDate.toLocalDate())) continue;
            if (toDate != null && !week.isBefore(toDate.toLocalDate())) continue;
            UUID repositoryId = row.repositoryId();
            String repositoryName = repositoryNames.getOrDefault(repositoryId, repositoryName(userId, repositoryId));
            YearMonth month = YearMonth.from(week);
            lineStatisticsCommitCount += row.commits();
            accumulateLines(years.computeIfAbsent(week.getYear(), ignored -> new PeriodAccumulator()), repositoryId, repositoryName, row);
            accumulateLines(months.computeIfAbsent(month, ignored -> new PeriodAccumulator()), repositoryId, repositoryName, row);
            accumulateLines(weeks.computeIfAbsent(week, ignored -> new PeriodAccumulator()), repositoryId, repositoryName, row);
            accumulateLines(projectMonths.computeIfAbsent(repositoryId, ignored -> new TreeMap<>())
                    .computeIfAbsent(month, ignored -> new PeriodAccumulator()), repositoryId, repositoryName, row);
            accumulateLines(projectMonthWeeks.computeIfAbsent(repositoryId, ignored -> new TreeMap<>())
                    .computeIfAbsent(month, ignored -> new TreeMap<>())
                    .computeIfAbsent(week, ignored -> new PeriodAccumulator()), repositoryId, repositoryName, row);
        }

        Object[] repositoryStats = entityManager.createQuery(
                "select coalesce(sum(r.userAdditions),0), coalesce(sum(r.userDeletions),0), " +
                "coalesce(sum(r.userCommitCount),0) from SourceRepository r " +
                "where r.user.id=:userId and r.includedInAnalysis=true", Object[].class)
                .setParameter("userId", userId).getSingleResult();
        long additions = ((Number) repositoryStats[0]).longValue();
        long deletions = ((Number) repositoryStats[1]).longValue();
        long measuredCommits = ((Number) repositoryStats[2]).longValue();
        double average = measuredCommits > 0 ? (double) (additions + deletions) / measuredCommits : 0.0;

        List<YearPoint> yearPoints = years.entrySet().stream().map(e -> toYearPoint(e.getKey(), e.getValue())).toList();
        List<MonthPoint> monthPoints = months.entrySet().stream().map(e -> toMonthPoint(e.getKey(), e.getValue())).toList();
        List<WeekPoint> weekPoints = weeks.entrySet().stream().map(e -> toWeekPoint(e.getKey(), e.getValue())).toList();
        int activeProjects = rows.stream().map(row -> (UUID) row[1]).collect(java.util.stream.Collectors.toSet()).size();

        List<Object[]> projectRows = entityManager.createQuery(
                "select c.repository.id, c.repository.name, min(c.occurredAt), max(c.occurredAt), count(c.id) " +
                "from Contribution c where c.user.id=:userId and c.type=:type " +
                "group by c.repository.id, c.repository.name order by min(c.occurredAt)", Object[].class)
                .setParameter("userId", userId).setParameter("type", Contribution.Type.COMMIT).getResultList();

        List<ProjectLifecycle> projectsOverTime = projectRows.stream().map(row -> {
            UUID repositoryId = (UUID) row[0];
            List<ProjectPeriodActivity> monthly = projectMonths.getOrDefault(repositoryId, Map.of()).entrySet().stream()
                    .map(e -> toProjectPeriod(e.getKey().toString(), e.getKey().toString(), e.getValue())).toList();
            List<ProjectPeriodActivity> weekly = projectMonthWeeks.getOrDefault(repositoryId, Map.of()).entrySet().stream()
                    .flatMap(monthEntry -> monthEntry.getValue().entrySet().stream()
                            .map(weekEntry -> toProjectPeriod(weekEntry.getKey().toString(),
                                    monthEntry.getKey().toString(), weekEntry.getValue())))
                    .sorted(Comparator.comparing(ProjectPeriodActivity::parentMonth).thenComparing(ProjectPeriodActivity::period)).toList();
            return new ProjectLifecycle(repositoryId, (String) row[1], (OffsetDateTime) row[2], (OffsetDateTime) row[3],
                    ((Number) row[4]).intValue(), projectTypes.getOrDefault(repositoryId, "Unclassified"),
                    technologies.getOrDefault(repositoryId, "Unclassified"), monthly, weekly);
        }).toList();

        return new ActivityResponse(rows.size(), activeProjects, average, 0.0, additions, deletions, first, last,
                yearPoints, monthPoints, weekPoints, projectsOverTime,
                measuredCommits > 0 || lineStatisticsCommitCount > 0, lineStatisticsCommitCount);
    }

    private String repositoryName(UUID userId, UUID repositoryId) {
        return entityManager.createQuery(
                "select r.name from SourceRepository r where r.user.id=:userId and r.id=:repositoryId", String.class)
                .setParameter("userId", userId).setParameter("repositoryId", repositoryId)
                .getResultStream().findFirst().orElse("Unknown project");
    }

    private void accumulateCommit(PeriodAccumulator a, UUID repositoryId, String repositoryName) {
        a.commits++; a.projectIds.add(repositoryId); a.projectNames.add(repositoryName);
    }

    private void accumulateLines(PeriodAccumulator a, UUID repositoryId, String repositoryName,
                                 RepositoryUserActivityWeekRepository.WeekRow row) {
        a.additions += row.additions(); a.deletions += row.deletions();
        a.lineStatisticsCommitCount += row.commits();
        a.projectIds.add(repositoryId); a.projectNames.add(repositoryName);
    }

    private YearPoint toYearPoint(int year, PeriodAccumulator v) { return new YearPoint(year, v.commits, v.additions, v.deletions, v.additions + v.deletions, v.lineStatisticsCommitCount, v.projectIds.size(), List.copyOf(v.projectNames)); }
    private MonthPoint toMonthPoint(YearMonth month, PeriodAccumulator v) { return new MonthPoint(month.toString(), v.commits, v.additions, v.deletions, v.additions + v.deletions, v.lineStatisticsCommitCount, v.projectIds.size(), List.copyOf(v.projectNames)); }
    private WeekPoint toWeekPoint(LocalDate week, PeriodAccumulator v) { return new WeekPoint(week.toString(), v.commits, v.additions, v.deletions, v.additions + v.deletions, v.lineStatisticsCommitCount, v.projectIds.size(), List.copyOf(v.projectNames)); }
    private ProjectPeriodActivity toProjectPeriod(String period, String parentMonth, PeriodAccumulator v) { return new ProjectPeriodActivity(period, parentMonth, v.commits, v.additions, v.deletions, v.additions + v.deletions, v.lineStatisticsCommitCount); }

    private Map<UUID, String> loadPrimaryProjectTypes(UUID userId) {
        List<Object[]> rows = entityManager.createQuery(
                "select c.repository.id, c.category.displayName, c.confidence from RepositoryProjectCategory c " +
                "where c.repository.user.id=:userId and c.repository.includedInAnalysis=true", Object[].class)
                .setParameter("userId", userId).getResultList();
        Map<UUID, RankedLabel> ranked = new HashMap<>();
        for (Object[] row : rows) {
            UUID id=(UUID)row[0]; String label=(String)row[1]; String confidence=row[2]==null?"LOW":row[2].toString();
            int rank=switch(confidence){case "HIGH"->3;case "MEDIUM"->2;default->1;};
            RankedLabel old=ranked.get(id); if(old==null||rank>old.rank||(rank==old.rank&&label.compareToIgnoreCase(old.label)<0)) ranked.put(id,new RankedLabel(label,rank));
        }
        Map<UUID,String> result=new HashMap<>(); ranked.forEach((id,v)->result.put(id,v.label)); return result;
    }

    private Map<UUID, String> loadPrimaryTechnologies(UUID userId) {
        List<Object[]> rows = entityManager.createQuery(
                "select e.repository.id, e.technology.displayName, e.strength from RepositoryTechnologyEvidence e " +
                "where e.user.id=:userId and e.repository.includedInAnalysis=true", Object[].class)
                .setParameter("userId", userId).getResultList();
        Map<UUID, RankedLabel> ranked=new HashMap<>();
        for(Object[] row:rows){UUID id=(UUID)row[0];String label=(String)row[1];String strength=row[2]==null?"EXPOSURE":row[2].toString();int rank=switch(strength){case "OBSERVED"->5;case "STRONG"->4;case "MODERATE"->3;case "LIMITED"->2;default->1;};RankedLabel old=ranked.get(id);if(old==null||rank>old.rank||(rank==old.rank&&label.compareToIgnoreCase(old.label)<0))ranked.put(id,new RankedLabel(label,rank));}
        Map<UUID,String> result=new HashMap<>();ranked.forEach((id,v)->result.put(id,v.label));return result;
    }

    private OffsetDateTime parseStart(String value) { return value==null||value.isBlank()?null:LocalDate.parse(value).atStartOfDay().atOffset(java.time.ZoneOffset.UTC); }
    private OffsetDateTime parseEnd(String value) { return value==null||value.isBlank()?null:LocalDate.parse(value).plusDays(1).atStartOfDay().atOffset(java.time.ZoneOffset.UTC); }

    private static class PeriodAccumulator { int commits; long additions; long deletions; int lineStatisticsCommitCount; Set<UUID> projectIds=new HashSet<>(); Set<String> projectNames=new TreeSet<>(); }
    private record RankedLabel(String label,int rank) {}

    public record ActivityResponse(int commitCount,int activeProjects,double averageCommitSize,double medianCommitSize,long additions,long deletions,OffsetDateTime firstActivityAt,OffsetDateTime lastActivityAt,List<YearPoint> commitsPerYear,List<MonthPoint> commitsPerMonth,List<WeekPoint> commitsPerWeek,List<ProjectLifecycle> projectsOverTime,boolean commitSizeStatisticsAvailable,int lineStatisticsCommitCount) {}
    public record YearPoint(int year,int commits,long additions,long deletions,long changedLines,int lineStatisticsCommitCount,int activeProjects,List<String> projects) {}
    public record MonthPoint(String month,int commits,long additions,long deletions,long changedLines,int lineStatisticsCommitCount,int activeProjects,List<String> projects) {}
    public record WeekPoint(String week,int commits,long additions,long deletions,long changedLines,int lineStatisticsCommitCount,int activeProjects,List<String> projects) {}
    public record ProjectLifecycle(UUID repositoryId,String repositoryName,OffsetDateTime firstActivityAt,OffsetDateTime lastActivityAt,int commits,String projectType,String technology,List<ProjectPeriodActivity> monthlyActivity,List<ProjectPeriodActivity> weeklyActivity) {}
    public record ProjectPeriodActivity(String period,String parentMonth,int commits,long additions,long deletions,long changedLines,int lineStatisticsCommitCount) {}
}
