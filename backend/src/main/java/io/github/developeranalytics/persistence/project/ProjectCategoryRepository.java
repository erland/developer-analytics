package io.github.developeranalytics.persistence.project;

import io.github.developeranalytics.domain.project.ProjectCategory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProjectCategoryRepository {

    @Inject
    EntityManager entityManager;

    public void persist(ProjectCategory category) {
        entityManager.persist(category);
    }

    public Optional<ProjectCategory> findByKey(String key) {
        return entityManager.createQuery(
                "select c from ProjectCategory c where c.categoryKey=:key",
                ProjectCategory.class)
            .setParameter("key", key)
            .getResultStream()
            .findFirst();
    }

    public List<ProjectCategory> findActive() {
        return entityManager.createQuery(
                "select c from ProjectCategory c " +
                "where c.active=true order by c.sortOrder, c.displayName",
                ProjectCategory.class)
            .getResultList();
    }
}
