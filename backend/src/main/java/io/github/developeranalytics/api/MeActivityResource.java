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

        List<Object[]> rows = entityManager.createQuery(
                "select c.occurredAt, c.additions, c.deletions, c.repository.id " +
                "from Contribution c " +
                "where c.user.id=:userId and c.type=:type " +
                "and (:fromDate is null or c.occurredAt >= :fromDate) " +
                "and (:toDate is null or c.occurredAt < :toDate) " +
                "order by c.occurredAt",
                Object[].class)
            .setParameter("userId", current.user().getId())
            .setParameter("type", io.github.developeranalytics.domain.model.Contribution.Type.COMMIT)
            .setParameter("fromDate", fromDate)
            .setParameter("toDate", toDate)
            .getResultList();

        Map<Integer, Integer> commitsPerYear = new TreeMap<>();
        Map<YearMonth, Integer> commitsPerMonth = new TreeMap<>();
        Map<YearMonth, Set<UUID>> activeProjectsPerMonth = new TreeMap<>();

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

            commitsPerYear.merge(occurredAt.getYear(), 1, Integer::sum);
            YearMonth month = YearMonth.from(occurredAt);
            commitsPerMonth.merge(month, 1, Integer::sum);
            activeProjectsPerMonth
                    .computeIfAbsent(month, ignored -> new HashSet<>())
                    .add(repositoryId);

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

        double average = sizes.isEmpty()
                ? 0.0
                : sizes.stream().mapToInt(Integer::intValue).average().orElse(0.0);

        double median = median(sizes);

        List<YearPoint> years = commitsPerYear.entrySet().stream()
                .map(entry -> new YearPoint(entry.getKey(), entry.getValue()))
                .toList();

        List<MonthPoint> months = commitsPerMonth.entrySet().stream()
                .map(entry -> new MonthPoint(
                        entry.getKey().toString(),
                        entry.getValue(),
                        activeProjectsPerMonth.getOrDefault(entry.getKey(), Set.of()).size()
                ))
                .toList();

        int activeProjects = activeProjectsPerMonth.values().stream()
                .flatMap(Set::stream)
                .collect(java.util.stream.Collectors.toSet())
                .size();

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
                months
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
            List<MonthPoint> commitsPerMonth
    ) {}

    public record YearPoint(int year, int commits) {}

    public record MonthPoint(
            String month,
            int commits,
            int activeProjects
    ) {}
}
