package io.github.developeranalytics.persistence.repository;

import io.github.developeranalytics.domain.model.RepositorySyncRun;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RepositorySyncRunRepository {

    @Inject
    EntityManager entityManager;

    public void persist(RepositorySyncRun run) {
        entityManager.persist(run);
    }

    public Optional<RepositorySyncRun> findByIdForUser(UUID id, UUID userId) {
        return entityManager.createQuery(
                "select r from RepositorySyncRun r where r.id=:id and r.user.id=:userId",
                RepositorySyncRun.class)
            .setParameter("id", id)
            .setParameter("userId", userId)
            .getResultStream()
            .findFirst();
    }

    public List<RepositorySyncRun> findRecentForUser(UUID userId) {
        return entityManager.createQuery(
                "select r from RepositorySyncRun r where r.user.id=:userId order by r.createdAt desc",
                RepositorySyncRun.class)
            .setParameter("userId", userId)
            .setMaxResults(20)
            .getResultList();
    }
}
