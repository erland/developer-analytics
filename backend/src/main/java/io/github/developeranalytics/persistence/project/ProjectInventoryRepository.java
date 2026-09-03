package io.github.developeranalytics.persistence.project;

import io.github.developeranalytics.domain.model.RepositoryOwnershipRelation;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import io.github.developeranalytics.persistence.repository.RepositoryUserActivityWeekRepository;

import java.time.LocalDate;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@ApplicationScoped
public class ProjectInventoryRepository {

    @Inject
    EntityManager entityManager;

    @Inject
    RepositoryUserActivityWeekRepository activityWeeks;

    public Page find(
            UUID userId,
            int page,
            int pageSize,
            String search,
            String ownership,
            String visibility,
            String activity,
            List<String> categoryKeys,
            List<String> technologyKeys,
            LocalDate activityFrom,
            LocalDate activityTo
    ) {
        FilterSpec filter = buildFilter(userId, search, ownership, visibility, activity,
                categoryKeys, technologyKeys, activityFrom, activityTo);

        int safePage = Math.max(page, 0);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        if (filter.noMatches()) {
            return new Page(List.of(), 0, safePage, safePageSize, List.of());
        }

        String orderBy = " order by r.lastActivityAt desc nulls last, r.name";
        TypedQuery<SourceRepository> query = entityManager.createQuery(
                "select r from SourceRepository r" + filter.where() + orderBy,
                SourceRepository.class);
        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(r.id) from SourceRepository r" + filter.where(),
                Long.class);
        TypedQuery<UUID> idsQuery = entityManager.createQuery(
                "select r.id from SourceRepository r" + filter.where(),
                UUID.class);

        filter.params().forEach((key, value) -> {
            query.setParameter(key, value);
            countQuery.setParameter(key, value);
            idsQuery.setParameter(key, value);
        });

        List<SourceRepository> items = query
                .setFirstResult(safePage * safePageSize)
                .setMaxResults(safePageSize)
                .getResultList();
        long total = countQuery.getSingleResult();
        List<UUID> matchingRepositoryIds = idsQuery.getResultList();

        return new Page(items, total, safePage, safePageSize, matchingRepositoryIds);
    }

    private FilterSpec buildFilter(
            UUID userId,
            String search,
            String ownership,
            String visibility,
            String activity,
            List<String> categoryKeys,
            List<String> technologyKeys,
            LocalDate activityFrom,
            LocalDate activityTo
    ) {
        StringBuilder where = new StringBuilder(" where r.user.id=:userId and r.includedInAnalysis=true ");
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("userId", userId);

        if (search != null && !search.isBlank()) {
            where.append(" and (lower(r.name) like :search or lower(coalesce(r.description,'')) like :search) ");
            params.put("search", "%" + search.trim().toLowerCase(Locale.ROOT) + "%");
        }

        if ("own".equalsIgnoreCase(ownership)) {
            where.append(" and r.ownershipRelation=:ownedByUser ");
            params.put("ownedByUser", RepositoryOwnershipRelation.OWNED_BY_USER);
        } else if ("external".equalsIgnoreCase(ownership)) {
            where.append(" and r.ownershipRelation<>:ownedByUser ");
            params.put("ownedByUser", RepositoryOwnershipRelation.OWNED_BY_USER);
        }

        if ("public".equalsIgnoreCase(visibility)) {
            where.append(" and r.visibility=:publicVisibility ");
            params.put("publicVisibility", RepositoryVisibility.PUBLIC);
        } else if ("private".equalsIgnoreCase(visibility)) {
            where.append(" and r.visibility=:privateVisibility ");
            params.put("privateVisibility", RepositoryVisibility.PRIVATE);
        }

        OffsetDateTime activeThreshold = OffsetDateTime.now(ZoneOffset.UTC).minusYears(1);
        if ("active".equalsIgnoreCase(activity)) {
            where.append(" and r.lastActivityAt>=:activeThreshold ");
            params.put("activeThreshold", activeThreshold);
        } else if ("inactive".equalsIgnoreCase(activity)) {
            where.append(" and (r.lastActivityAt is null or r.lastActivityAt<:activeThreshold) ");
            params.put("activeThreshold", activeThreshold);
        }

        List<String> normalizedCategoryKeys = normalizedKeys(categoryKeys);
        if (!normalizedCategoryKeys.isEmpty()) {
            where.append(" and exists (select 1 from RepositoryProjectCategory pc " +
                    "where pc.repository.id=r.id and pc.category.categoryKey in :categoryKeys) ");
            params.put("categoryKeys", normalizedCategoryKeys);
        }

        List<String> normalizedTechnologyKeys = normalizedKeys(technologyKeys);
        if (!normalizedTechnologyKeys.isEmpty()) {
            where.append(" and exists (select 1 from RepositoryTechnologyEvidence te " +
                    "where te.repository.id=r.id and te.technology.technologyKey in :technologyKeys) ");
            params.put("technologyKeys", normalizedTechnologyKeys);
        }

        if (activityFrom != null || activityTo != null) {
            List<UUID> activeRepositoryIds = activityWeeks.findActiveRepositoryIds(userId, activityFrom, activityTo);
            if (activeRepositoryIds.isEmpty()) return new FilterSpec("", Map.of(), true);
            where.append(" and r.id in :activityRepositoryIds ");
            params.put("activityRepositoryIds", activeRepositoryIds);
        }

        return new FilterSpec(where.toString(), params, false);
    }


    public List<OwnershipFacetRow> ownershipFacets(List<UUID> repositoryIds) {
        if (repositoryIds == null || repositoryIds.isEmpty()) return List.of();
        return entityManager.createQuery(
                "select r.ownershipRelation, count(r.id) from SourceRepository r " +
                "where r.id in :repositoryIds group by r.ownershipRelation order by r.ownershipRelation",
                Object[].class)
            .setParameter("repositoryIds", repositoryIds)
            .getResultList().stream()
            .map(row -> new OwnershipFacetRow(
                    row[0] == null ? "UNKNOWN" : row[0].toString(),
                    ((Number) row[1]).longValue()))
            .toList();
    }

    private List<String> normalizedKeys(List<String> values) {
        if (values == null) return List.of();
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    public record OwnershipFacetRow(String key, long count) {}

    private record FilterSpec(String where, Map<String, Object> params, boolean noMatches) {}

    public record Page(
            List<SourceRepository> items,
            long total,
            int page,
            int pageSize,
            List<UUID> matchingRepositoryIds
    ) {}
}
