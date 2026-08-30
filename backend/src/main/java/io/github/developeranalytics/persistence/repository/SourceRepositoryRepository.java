package io.github.developeranalytics.persistence.repository;

import io.github.developeranalytics.domain.model.SourceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SourceRepositoryRepository {

    @Inject
    EntityManager entityManager;

    public void persist(SourceRepository repository) {
        entityManager.persist(repository);
    }

    public Optional<SourceRepository> findByExternalIdForUser(
            UUID userId,
            String provider,
            String externalRepositoryId
    ) {
        return entityManager.createQuery(
                "select r from SourceRepository r " +
                "where r.user.id = :userId and r.provider = :provider " +
                "and r.externalRepositoryId = :externalRepositoryId",
                SourceRepository.class)
            .setParameter("userId", userId)
            .setParameter("provider", provider)
            .setParameter("externalRepositoryId", externalRepositoryId)
            .getResultStream()
            .findFirst();
    }

    public List<SourceRepository> findContributionSyncCandidates(
            UUID userId,
            int offset,
            int limit
    ) {
        return entityManager.createQuery(
                "select r from SourceRepository r " +
                "where r.user.id=:userId " +
                "and r.syncStatus <> :accessRevoked " +
                "order by r.lastActivityAt desc nulls last, r.name",
                SourceRepository.class)
            .setParameter("userId", userId)
            .setParameter("accessRevoked",
                    io.github.developeranalytics.domain.model.RepositorySyncStatus.ACCESS_REVOKED)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
    }

    public List<SourceRepository> findByUser(UUID userId) {
        return findAllForUser(userId);
    }

    public List<SourceRepository> findAllForUser(UUID userId) {
        return entityManager.createQuery(
                "select r from SourceRepository r " +
                "where r.user.id = :userId " +
                "order by r.lastActivityAt desc nulls last, r.name",
                SourceRepository.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    public Optional<SourceRepository> findByIdForUser(UUID repositoryId, UUID userId) {
        return entityManager.createQuery(
                "select r from SourceRepository r " +
                "where r.id = :repositoryId and r.user.id = :userId",
                SourceRepository.class)
            .setParameter("repositoryId", repositoryId)
            .setParameter("userId", userId)
            .getResultStream()
            .findFirst();
    }
}
