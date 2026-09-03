package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.*;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.persistence.project.ProjectInventoryRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Path("/api/me/contributions")
@Produces(MediaType.APPLICATION_JSON)
public class MeContributionsResource {
    @Inject CurrentUserService currentUserService;
    @Inject EntityManager entityManager;
    @Inject ProjectInventoryRepository inventory;

    @GET
    public Summary list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            @QueryParam("limit") @DefaultValue("100") int limit,
            @QueryParam("search") String search,
            @QueryParam("ownership") String ownership,
            @QueryParam("visibility") String visibility,
            @QueryParam("category") List<String> legacyCategories,
            @QueryParam("projectType") List<String> projectTypes,
            @QueryParam("technology") List<String> technologies,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("year") Integer year,
            @QueryParam("month") String month,
            @QueryParam("week") String week
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        UUID userId = current.user().getId();
        var period = AnalysisPeriod.resolve(from, to, year, month, week);

        List<String> selectedProjectTypes = new ArrayList<>();
        if (legacyCategories != null) selectedProjectTypes.addAll(legacyCategories);
        if (projectTypes != null) selectedProjectTypes.addAll(projectTypes);

        var matchingProjects = inventory.find(
                userId,
                0,
                1,
                search,
                ownership,
                visibility,
                null,
                selectedProjectTypes,
                technologies,
                period.from(),
                period.to()
        );

        List<UUID> repositoryIds = matchingProjects.matchingRepositoryIds();
        if (repositoryIds.isEmpty()) {
            return new Summary(0, 0, 0, 0, 0, List.of());
        }

        ContributionFilter contributionFilter = contributionFilter(userId, repositoryIds, period);
        String where = contributionFilter.where();

        Query countQuery = entityManager.createQuery(
                "select c.type, count(c.id) from Contribution c" + where + " group by c.type",
                Object[].class
        );
        contributionFilter.params().forEach(countQuery::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> counts = countQuery.getResultList();
        EnumMap<Contribution.Type, Long> byType = new EnumMap<>(Contribution.Type.class);
        for (Object[] row : counts) {
            byType.put((Contribution.Type) row[0], ((Number) row[1]).longValue());
        }

        int safeLimit = Math.max(1, Math.min(limit, 500));
        Query recentQuery = entityManager.createQuery(
                "select c.repository.id, c.repository.name, max(c.occurredAt), count(c.id), " +
                "sum(case when c.type=:commitType then 1 else 0 end) " +
                "from Contribution c" + where +
                " group by c.repository.id, c.repository.name order by max(c.occurredAt) desc",
                Object[].class
        );
        contributionFilter.params().forEach(recentQuery::setParameter);
        recentQuery.setParameter("commitType", Contribution.Type.COMMIT);
        recentQuery.setMaxResults(safeLimit);

        @SuppressWarnings("unchecked")
        List<Object[]> recentRows = recentQuery.getResultList();
        List<RecentProject> recentProjects = recentRows.stream()
                .map(r -> new RecentProject(
                        (UUID) r[0],
                        (String) r[1],
                        (OffsetDateTime) r[2],
                        ((Number) r[3]).longValue(),
                        r[4] == null ? 0 : ((Number) r[4]).longValue()
                ))
                .toList();

        long commits = byType.getOrDefault(Contribution.Type.COMMIT, 0L);
        long prs = byType.getOrDefault(Contribution.Type.PULL_REQUEST, 0L);
        long reviews = byType.getOrDefault(Contribution.Type.REVIEW, 0L);
        long issues = byType.getOrDefault(Contribution.Type.ISSUE, 0L);
        return new Summary(commits + prs + reviews + issues, commits, prs, reviews, issues, recentProjects);
    }

    private ContributionFilter contributionFilter(
            UUID userId,
            List<UUID> repositoryIds,
            AnalysisPeriod.Range period
    ) {
        StringBuilder where = new StringBuilder(
                " where c.user.id=:userId and c.repository.includedInAnalysis=true and c.repository.id in :repositoryIds"
        );
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("userId", userId);
        params.put("repositoryIds", repositoryIds);

        if (period.from() != null) {
            where.append(" and c.occurredAt>=:fromAt");
            params.put("fromAt", atStartOfDay(period.from()));
        }
        if (period.to() != null) {
            where.append(" and c.occurredAt<:toExclusive");
            params.put("toExclusive", atStartOfDay(period.to().plusDays(1)));
        }

        return new ContributionFilter(where.toString(), params);
    }

    private OffsetDateTime atStartOfDay(LocalDate date) {
        return date.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    private record ContributionFilter(String where, Map<String, Object> params) {}

    public record Summary(long total, long commits, long pullRequests, long reviews, long issues,
                          List<RecentProject> recentProjects) {}
    public record RecentProject(UUID repositoryId, String repositoryName, OffsetDateTime lastActivityAt,
                                long contributionCount, long commitCount) {}
}
