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

    public void deleteDeterministicForRepository(UUID repositoryId) {
        entityManager.createQuery(
                "delete from RepositoryProjectCategory c " +
                "where c.repository.id=:repositoryId and c.source=:source")
            .setParameter("repositoryId", repositoryId)
            .setParameter("source", RepositoryProjectCategory.Source.DETERMINISTIC)
            .executeUpdate();
    }
}
