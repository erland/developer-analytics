package io.github.developeranalytics.persistence.repository;

import io.github.developeranalytics.domain.model.ProviderSyncRun;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProviderSyncRunRepository {
    @Inject EntityManager em;

    public void persist(ProviderSyncRun run) { em.persist(run); }

    public Optional<ProviderSyncRun> findById(UUID id) {
        return Optional.ofNullable(em.find(ProviderSyncRun.class, id));
    }

    public Optional<ProviderSyncRun> findByIdForUpdate(UUID id) {
        return Optional.ofNullable(em.find(ProviderSyncRun.class, id, LockModeType.PESSIMISTIC_WRITE));
    }

    public Optional<ProviderSyncRun> findByIdForUser(UUID id, UUID userId) {
        return em.createQuery("select r from ProviderSyncRun r where r.id=:id and r.user.id=:userId", ProviderSyncRun.class)
                .setParameter("id", id).setParameter("userId", userId).getResultStream().findFirst();
    }

    public List<ProviderSyncRun> findRecentForUser(UUID userId) {
        return em.createQuery("select r from ProviderSyncRun r where r.user.id=:userId order by r.startedAt desc", ProviderSyncRun.class)
                .setParameter("userId", userId).setMaxResults(20).getResultList();
    }
}
