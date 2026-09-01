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


    public List<BackgroundJob> findRecentForUser(UUID userId, int limit) {
        return em.createQuery(
                "select j from BackgroundJob j where j.user.id=:userId " +
                "order by j.createdAt desc", BackgroundJob.class)
            .setParameter("userId", userId)
            .setMaxResults(Math.max(1, Math.min(limit, 200)))
            .getResultList();
    }

    public List<BackgroundJob> findRecentErrorsForUser(UUID userId, int limit) {
        return em.createQuery(
                "select j from BackgroundJob j where j.user.id=:userId " +
                "and j.lastError is not null order by j.createdAt desc", BackgroundJob.class)
            .setParameter("userId", userId)
            .setMaxResults(Math.max(1, Math.min(limit, 200)))
            .getResultList();
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

@Transactional
public int cancelProviderJobs(
        UUID userId,
        String provider,
        OffsetDateTime now
) {
    return em.createNativeQuery(
            "UPDATE background_job SET status='CANCELLED', " +
            "completed_at=:now, locked_at=NULL, locked_by=NULL, " +
            "last_error='Cancelled because provider was disconnected' " +
            "WHERE user_id=:userId " +
            "AND status IN ('QUEUED','WAITING') " +
            "AND (payload->>'provider'=:provider " +
            "OR deduplication_key LIKE :providerPrefix)"
    )
    .setParameter("now", now)
    .setParameter("userId", userId)
    .setParameter("provider", provider)
    .setParameter("providerPrefix", provider + ":%")
    .executeUpdate();
}


@Transactional
public int recoverStaleRunningJobs(
        OffsetDateTime lockedBefore,
        OffsetDateTime nextExecutionAt
) {
    @SuppressWarnings("unchecked")
    List<UUID> ids = em.createNativeQuery(
            "SELECT id FROM background_job " +
            "WHERE status='RUNNING' AND locked_at < :lockedBefore " +
            "FOR UPDATE SKIP LOCKED"
    )
    .setParameter("lockedBefore", lockedBefore)
    .getResultList();

    int recovered = 0;
    for (UUID id : ids) {
        BackgroundJob job = em.find(BackgroundJob.class, id);
        if (job != null && job.getStatus() == BackgroundJobStatus.RUNNING) {
            job.recoverInterrupted(nextExecutionAt);
            recovered++;
        }
    }
    em.flush();
    return recovered;
}

}
