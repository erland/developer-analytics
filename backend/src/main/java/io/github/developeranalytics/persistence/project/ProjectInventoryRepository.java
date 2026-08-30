package io.github.developeranalytics.persistence.project;

import io.github.developeranalytics.domain.model.RepositoryOwnershipRelation;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

@ApplicationScoped
public class ProjectInventoryRepository {

    @Inject
    EntityManager entityManager;

    public Page find(
            UUID userId,
            int page,
            int pageSize,
            String search,
            String ownership,
            String visibility,
            String activity,
            String categoryKey,
            String technologyKey
    ) {
        StringBuilder where = new StringBuilder(" where r.user.id=:userId ");
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

        if (categoryKey != null && !categoryKey.isBlank()) {
            where.append(" and exists (" +
                    "select 1 from RepositoryProjectCategory pc " +
                    "where pc.repository.id=r.id and pc.category.categoryKey=:categoryKey) ");
            params.put("categoryKey", categoryKey);
        }

        if (technologyKey != null && !technologyKey.isBlank()) {
            where.append(" and exists (" +
                    "select 1 from RepositoryTechnologyEvidence te " +
                    "where te.repository.id=r.id and te.technology.technologyKey=:technologyKey) ");
            params.put("technologyKey", technologyKey);
        }

        String orderBy = " order by r.lastActivityAt desc nulls last, r.name";

        TypedQuery<SourceRepository> query = entityManager.createQuery(
                "select r from SourceRepository r" + where + orderBy,
                SourceRepository.class);

        TypedQuery<Long> countQuery = entityManager.createQuery(
                "select count(r.id) from SourceRepository r" + where,
                Long.class);

        params.forEach((key, value) -> {
            query.setParameter(key, value);
            countQuery.setParameter(key, value);
        });

        int safePage = Math.max(page, 0);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);

        List<SourceRepository> items = query
                .setFirstResult(safePage * safePageSize)
                .setMaxResults(safePageSize)
                .getResultList();

        long total = countQuery.getSingleResult();

        return new Page(items, total, safePage, safePageSize);
    }

    public record Page(
            List<SourceRepository> items,
            long total,
            int page,
            int pageSize
    ) {}
}
