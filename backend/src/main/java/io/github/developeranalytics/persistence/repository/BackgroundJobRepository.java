package io.github.developeranalytics.persistence.repository;

import io.github.developeranalytics.domain.job.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.*;

@ApplicationScoped
public class BackgroundJobRepository {
    @Inject EntityManager em;

    @Transactional
    public void persist(BackgroundJob job) {
        em.persist(job);
    }


    public boolean existsActiveDeduplicatedJob(
            java.util.UUID userId,
            String deduplicationKey
    ) {
        Long count = em.createQuery(
                "select count(j) from BackgroundJob j " +
                "where j.user.id=:userId and j.deduplicationKey=:key " +
                "and j.status in (:queued, :waiting, :running)",
                Long.class)
            .setParameter("userId", userId)
            .setParameter("key", deduplicationKey)
            .setParameter("queued", BackgroundJobStatus.QUEUED)
            .setParameter("waiting", BackgroundJobStatus.WAITING)
            .setParameter("running", BackgroundJobStatus.RUNNING)
            .getSingleResult();

        return count != null && count > 0;
    }

    @Transactional
    public Optional<BackgroundJob> claimNext(String workerId, OffsetDateTime now) {
        @SuppressWarnings("unchecked")
        List<UUID> ids = em.createNativeQuery(
            "SELECT id FROM background_job WHERE status IN ('QUEUED','WAITING') " +
            "AND next_execution_at <= :now AND locked_at IS NULL " +
            "ORDER BY priority ASC, created_at ASC FOR UPDATE SKIP LOCKED LIMIT 1")
            .setParameter("now", now).getResultList();
        if(ids.isEmpty()) return Optional.empty();
        BackgroundJob job = em.find(BackgroundJob.class, ids.get(0));
        job.markRunning(workerId, now);
        em.flush();
        return Optional.of(job);
    }
}
