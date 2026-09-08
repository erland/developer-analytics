package io.github.developeranalytics.service.activity;

import io.github.developeranalytics.domain.change.ChangeKind;
import io.github.developeranalytics.persistence.project.ProjectInventoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.*;

/**
 * Aggregates filtered commit activity from persisted per-file change rows.
 *
 * <p>Each commit is grouped before aggregation, so a mixed commit touching several selected
 * change kinds counts once. Additions/deletions are summed only from matching file rows and are
 * therefore never double-counted.</p>
 */
@ApplicationScoped
public class ChangeKindActivityService {
    @Inject EntityManager entityManager;
    @Inject ProjectInventoryRepository projectInventory;

    @Transactional
    public ActivityApplicationService.ActivityResult get(
            UUID userId,
            LocalDate from,
            LocalDate to,
            String search,
            String ownership,
            String visibility,
            List<String> selectedProjectTypes,
            List<String> technologiesFilter,
            Set<ChangeKind> changeKinds
    ) {
        OffsetDateTime fromDate = from == null ? null : from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toDate = to == null ? null : to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);
        var matchingProjects = projectInventory.find(userId, 0, 1, search, ownership, visibility, null,
                selectedProjectTypes, technologiesFilter, from, to);
        Set<UUID> matchingRepositoryIds = new HashSet<>(matchingProjects.matchingRepositoryIds());
        if (matchingRepositoryIds.isEmpty()) return empty();

        StringBuilder jpql = new StringBuilder(
                "select f.contribution.id, f.occurredAt, f.repository.id, f.repository.name, " +
                "sum(f.additions), sum(f.deletions) from ContributionFileChange f " +
                "where f.user.id=:userId and f.repository.id in :repositoryIds and f.changeKind in :changeKinds");
        if (fromDate != null) jpql.append(" and f.occurredAt>=:fromDate");
        if (toDate != null) jpql.append(" and f.occurredAt<:toDate");
        jpql.append(" group by f.contribution.id, f.occurredAt, f.repository.id, f.repository.name order by f.occurredAt");
        var query = entityManager.createQuery(jpql.toString(), Object[].class)
                .setParameter("userId", userId)
                .setParameter("repositoryIds", matchingRepositoryIds)
                .setParameter("changeKinds", changeKinds);
        if (fromDate != null) query.setParameter("fromDate", fromDate);
        if (toDate != null) query.setParameter("toDate", toDate);
        List<Object[]> rows = query.getResultList();
        if (rows.isEmpty()) return empty();

        Map<Integer, Accumulator> years = new TreeMap<>();
        Map<YearMonth, Accumulator> months = new TreeMap<>();
        Map<LocalDate, Accumulator> weeks = new TreeMap<>();
        Map<UUID, Map<YearMonth, Accumulator>> projectMonths = new HashMap<>();
        Map<UUID, Map<YearMonth, Map<LocalDate, Accumulator>>> projectMonthWeeks = new HashMap<>();
        Map<UUID, ProjectAccumulator> projects = new LinkedHashMap<>();

        long additions = 0;
        long deletions = 0;
        OffsetDateTime first = null;
        OffsetDateTime last = null;

        for (Object[] row : rows) {
            OffsetDateTime at = (OffsetDateTime) row[1];
            UUID repositoryId = (UUID) row[2];
            String repositoryName = (String) row[3];
            long commitAdditions = ((Number) row[4]).longValue();
            long commitDeletions = ((Number) row[5]).longValue();
            additions += commitAdditions;
            deletions += commitDeletions;
            if (first == null || at.isBefore(first)) first = at;
            if (last == null || at.isAfter(last)) last = at;

            YearMonth month = YearMonth.from(at);
            LocalDate week = at.toLocalDate().minusDays(at.getDayOfWeek().getValue() - 1L);
            add(years.computeIfAbsent(at.getYear(), ignored -> new Accumulator()), repositoryId, repositoryName, commitAdditions, commitDeletions);
            add(months.computeIfAbsent(month, ignored -> new Accumulator()), repositoryId, repositoryName, commitAdditions, commitDeletions);
            add(weeks.computeIfAbsent(week, ignored -> new Accumulator()), repositoryId, repositoryName, commitAdditions, commitDeletions);
            add(projectMonths.computeIfAbsent(repositoryId, ignored -> new TreeMap<>()).computeIfAbsent(month, ignored -> new Accumulator()), repositoryId, repositoryName, commitAdditions, commitDeletions);
            add(projectMonthWeeks.computeIfAbsent(repositoryId, ignored -> new TreeMap<>())
                    .computeIfAbsent(month, ignored -> new TreeMap<>()).computeIfAbsent(week, ignored -> new Accumulator()),
                    repositoryId, repositoryName, commitAdditions, commitDeletions);
            projects.computeIfAbsent(repositoryId, ignored -> new ProjectAccumulator(repositoryName, at))
                    .record(at);
        }

        Map<UUID, String> projectTypes = loadPrimaryProjectTypes(userId);
        Map<UUID, String> technologies = loadPrimaryTechnologies(userId);
        Map<UUID, List<String>> allProjectTypes = loadProjectTypes(userId);
        Map<UUID, List<String>> allTechnologies = loadTechnologies(userId);

        List<ActivityApplicationService.YearPoint> yearPoints = years.entrySet().stream()
                .map(e -> new ActivityApplicationService.YearPoint(e.getKey(), e.getValue().commits, e.getValue().additions,
                        e.getValue().deletions, e.getValue().additions + e.getValue().deletions, e.getValue().commits,
                        e.getValue().projectIds.size(), List.copyOf(e.getValue().projectNames))).toList();
        List<ActivityApplicationService.MonthPoint> monthPoints = months.entrySet().stream()
                .map(e -> new ActivityApplicationService.MonthPoint(e.getKey().toString(), e.getValue().commits, e.getValue().additions,
                        e.getValue().deletions, e.getValue().additions + e.getValue().deletions, e.getValue().commits,
                        e.getValue().projectIds.size(), List.copyOf(e.getValue().projectNames))).toList();
        List<ActivityApplicationService.WeekPoint> weekPoints = weeks.entrySet().stream()
                .map(e -> new ActivityApplicationService.WeekPoint(e.getKey().toString(), e.getValue().commits, e.getValue().additions,
                        e.getValue().deletions, e.getValue().additions + e.getValue().deletions, e.getValue().commits,
                        e.getValue().projectIds.size(), List.copyOf(e.getValue().projectNames))).toList();

        List<ActivityApplicationService.ProjectLifecycle> projectRows = projects.entrySet().stream().map(entry -> {
            UUID repositoryId = entry.getKey();
            ProjectAccumulator project = entry.getValue();
            List<ActivityApplicationService.ProjectPeriodActivity> monthly = projectMonths.getOrDefault(repositoryId, Map.of()).entrySet().stream()
                    .map(e -> period(e.getKey().toString(), e.getKey().toString(), e.getValue())).toList();
            List<ActivityApplicationService.ProjectPeriodActivity> weekly = projectMonthWeeks.getOrDefault(repositoryId, Map.of()).entrySet().stream()
                    .flatMap(monthEntry -> monthEntry.getValue().entrySet().stream()
                            .map(weekEntry -> period(weekEntry.getKey().toString(), monthEntry.getKey().toString(), weekEntry.getValue())))
                    .sorted(Comparator.comparing(ActivityApplicationService.ProjectPeriodActivity::parentMonth)
                            .thenComparing(ActivityApplicationService.ProjectPeriodActivity::period)).toList();
            return new ActivityApplicationService.ProjectLifecycle(repositoryId, project.name, project.first, project.last, project.commits,
                    projectTypes.getOrDefault(repositoryId, "Unclassified"), technologies.getOrDefault(repositoryId, "Unclassified"),
                    allProjectTypes.getOrDefault(repositoryId, List.of()), allTechnologies.getOrDefault(repositoryId, List.of()), monthly, weekly);
        }).toList();

        double average = rows.isEmpty() ? 0.0 : (double) (additions + deletions) / rows.size();
        return new ActivityApplicationService.ActivityResult(rows.size(), projects.size(), average, 0.0, additions, deletions,
                first, last, yearPoints, monthPoints, weekPoints, projectRows, true, rows.size());
    }

    private ActivityApplicationService.ActivityResult empty() {
        return new ActivityApplicationService.ActivityResult(0, 0, 0.0, 0.0, 0, 0, null, null,
                List.of(), List.of(), List.of(), List.of(), false, 0);
    }

    private void add(Accumulator a, UUID repositoryId, String repositoryName, long additions, long deletions) {
        a.commits++;
        a.additions += additions;
        a.deletions += deletions;
        a.projectIds.add(repositoryId);
        a.projectNames.add(repositoryName);
    }

    private ActivityApplicationService.ProjectPeriodActivity period(String period, String parentMonth, Accumulator a) {
        return new ActivityApplicationService.ProjectPeriodActivity(period, parentMonth, a.commits, a.additions, a.deletions,
                a.additions + a.deletions, a.commits);
    }

    private Map<UUID, String> loadPrimaryProjectTypes(UUID userId) {
        List<Object[]> rows = entityManager.createQuery(
                "select c.repository.id, c.category.displayName, c.confidence from RepositoryProjectCategory c " +
                        "where c.repository.user.id=:userId and c.repository.includedInAnalysis=true", Object[].class)
                .setParameter("userId", userId).getResultList();
        Map<UUID, RankedLabel> ranked = new HashMap<>();
        for (Object[] row : rows) {
            UUID id = (UUID) row[0]; String label = (String) row[1]; String confidence = row[2] == null ? "LOW" : row[2].toString();
            int rank = switch (confidence) { case "HIGH" -> 3; case "MEDIUM" -> 2; default -> 1; };
            RankedLabel old = ranked.get(id);
            if (old == null || rank > old.rank || (rank == old.rank && label.compareToIgnoreCase(old.label) < 0)) ranked.put(id, new RankedLabel(label, rank));
        }
        Map<UUID, String> result = new HashMap<>(); ranked.forEach((id, value) -> result.put(id, value.label)); return result;
    }

    private Map<UUID, String> loadPrimaryTechnologies(UUID userId) {
        List<Object[]> rows = entityManager.createQuery(
                "select e.repository.id, e.technology.displayName, e.strength from RepositoryTechnologyEvidence e " +
                        "where e.user.id=:userId and e.repository.includedInAnalysis=true", Object[].class)
                .setParameter("userId", userId).getResultList();
        Map<UUID, RankedLabel> ranked = new HashMap<>();
        for (Object[] row : rows) {
            UUID id = (UUID) row[0]; String label = (String) row[1]; String strength = row[2] == null ? "EXPOSURE" : row[2].toString();
            int rank = switch (strength) { case "OBSERVED" -> 5; case "STRONG" -> 4; case "MODERATE" -> 3; case "LIMITED" -> 2; default -> 1; };
            RankedLabel old = ranked.get(id);
            if (old == null || rank > old.rank || (rank == old.rank && label.compareToIgnoreCase(old.label) < 0)) ranked.put(id, new RankedLabel(label, rank));
        }
        Map<UUID, String> result = new HashMap<>(); ranked.forEach((id, value) -> result.put(id, value.label)); return result;
    }

    private Map<UUID, List<String>> loadProjectTypes(UUID userId) {
        return collectLabels(entityManager.createQuery(
                "select distinct c.repository.id, c.category.displayName from RepositoryProjectCategory c " +
                        "where c.repository.user.id=:userId and c.repository.includedInAnalysis=true", Object[].class)
                .setParameter("userId", userId).getResultList());
    }

    private Map<UUID, List<String>> loadTechnologies(UUID userId) {
        return collectLabels(entityManager.createQuery(
                "select distinct e.repository.id, e.technology.displayName from RepositoryTechnologyEvidence e " +
                        "where e.user.id=:userId and e.repository.includedInAnalysis=true", Object[].class)
                .setParameter("userId", userId).getResultList());
    }

    private Map<UUID, List<String>> collectLabels(List<Object[]> rows) {
        Map<UUID, SortedSet<String>> labels = new HashMap<>();
        for (Object[] row : rows) {
            if (row[1] == null || row[1].toString().isBlank()) continue;
            labels.computeIfAbsent((UUID) row[0], ignored -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER)).add(row[1].toString());
        }
        Map<UUID, List<String>> result = new HashMap<>(); labels.forEach((id, values) -> result.put(id, List.copyOf(values))); return result;
    }

    private static class Accumulator { int commits; long additions; long deletions; Set<UUID> projectIds = new HashSet<>(); Set<String> projectNames = new TreeSet<>(); }
    private static class ProjectAccumulator {
        final String name; OffsetDateTime first; OffsetDateTime last; int commits;
        ProjectAccumulator(String name, OffsetDateTime at) { this.name = name; this.first = at; this.last = at; }
        void record(OffsetDateTime at) { commits++; if (at.isBefore(first)) first = at; if (at.isAfter(last)) last = at; }
    }
    private record RankedLabel(String label, int rank) {}
}
