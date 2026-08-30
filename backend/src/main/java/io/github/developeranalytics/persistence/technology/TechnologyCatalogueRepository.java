package io.github.developeranalytics.persistence.technology;

import io.github.developeranalytics.domain.technology.TechnologyCatalogueEntry;
import io.github.developeranalytics.domain.technology.TechnologyCategory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class TechnologyCatalogueRepository {

    @Inject
    EntityManager entityManager;

    public void persist(TechnologyCatalogueEntry entry) {
        entityManager.persist(entry);
    }

    public Optional<TechnologyCatalogueEntry> findById(UUID id) {
        return Optional.ofNullable(
                entityManager.find(TechnologyCatalogueEntry.class, id)
        );
    }

    public Optional<TechnologyCatalogueEntry> findByKey(String technologyKey) {
        return entityManager.createQuery(
                "select t from TechnologyCatalogueEntry t where t.technologyKey=:key",
                TechnologyCatalogueEntry.class)
            .setParameter("key", technologyKey)
            .getResultStream()
            .findFirst();
    }

    public List<TechnologyCatalogueEntry> findAllActive() {
        return findActive();
    }

    public List<TechnologyCatalogueEntry> findActive() {
        return entityManager.createQuery(
                "select t from TechnologyCatalogueEntry t " +
                "where t.active=true order by t.category, t.displayName",
                TechnologyCatalogueEntry.class)
            .getResultList();
    }

    public List<TechnologyCatalogueEntry> findActiveByCategory(TechnologyCategory category) {
        return entityManager.createQuery(
                "select t from TechnologyCatalogueEntry t " +
                "where t.active=true and t.category=:category " +
                "order by t.displayName",
                TechnologyCatalogueEntry.class)
            .setParameter("category", category)
            .getResultList();
    }
}
