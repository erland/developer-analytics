package io.github.developeranalytics.persistence.project;

import io.github.developeranalytics.domain.project.RepositoryProjectCategory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RepositoryProjectCategoryRepository {
    @Inject EntityManager entityManager;

    public void persist(RepositoryProjectCategory classification) {
        entityManager.persist(classification);
    }

    public List<RepositoryProjectCategory> findForRepository(UUID repositoryId) {
        return entityManager.createQuery(
                "select c from RepositoryProjectCategory c join fetch c.category " +
                "where c.repository.id=:repositoryId " +
                "order by c.category.sortOrder, c.category.displayName",
                RepositoryProjectCategory.class)
            .setParameter("repositoryId", repositoryId)
            .getResultList();
    }


    public List<CategoryFacetRow> summarizeForRepositories(List<UUID> repositoryIds) {
        if (repositoryIds == null || repositoryIds.isEmpty()) return List.of();
        return entityManager.createQuery(
                "select c.category.categoryKey, c.category.displayName, count(distinct c.repository.id) " +
                "from RepositoryProjectCategory c where c.repository.id in :repositoryIds " +
                "group by c.category.categoryKey, c.category.displayName order by c.category.displayName",
                Object[].class)
            .setParameter("repositoryIds", repositoryIds)
            .getResultList().stream()
            .map(row -> new CategoryFacetRow((String) row[0], (String) row[1], ((Number) row[2]).longValue()))
            .toList();
    }

    public record CategoryFacetRow(String key, String name, long count) {}

    public void deleteDeterministicForRepository(UUID repositoryId) {
        entityManager.createQuery(
                "delete from RepositoryProjectCategory c " +
                "where c.repository.id=:repositoryId and c.source=:source")
            .setParameter("repositoryId", repositoryId)
            .setParameter("source", RepositoryProjectCategory.Source.DETERMINISTIC)
            .executeUpdate();
    }
}
