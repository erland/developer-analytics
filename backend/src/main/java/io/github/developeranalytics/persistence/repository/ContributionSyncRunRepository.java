package io.github.developeranalytics.persistence.repository;

import io.github.developeranalytics.domain.model.ContributionSyncRun;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ContributionSyncRunRepository {
    @Inject
    EntityManager entityManager;

    public void persist(ContributionSyncRun run) {
        entityManager.persist(run);
    }

    public Optional<ContributionSyncRun> findByIdForUser(UUID id, UUID userId) {
        return entityManager.createQuery(
                "select r from ContributionSyncRun r join fetch r.repository " +
                "where r.id=:id and r.user.id=:userId",
                ContributionSyncRun.class)
            .setParameter("id", id)
            .setParameter("userId", userId)
            .getResultStream()
            .findFirst();
    }

    public List<ContributionSyncRun> findRecentForUser(UUID userId) {
        return entityManager.createQuery(
                "select r from ContributionSyncRun r join fetch r.repository " +
                "where r.user.id=:userId order by r.createdAt desc",
                ContributionSyncRun.class)
            .setParameter("userId", userId)
            .setMaxResults(50)
            .getResultList();
    }

    public List<ContributionSyncRun> findRecentForRepository(UUID userId, UUID repositoryId) {
        return entityManager.createQuery(
                "select r from ContributionSyncRun r join fetch r.repository " +
                "where r.user.id=:userId and r.repository.id=:repositoryId " +
                "order by r.createdAt desc",
                ContributionSyncRun.class)
            .setParameter("userId", userId)
            .setParameter("repositoryId", repositoryId)
            .setMaxResults(20)
            .getResultList();
    }
}
