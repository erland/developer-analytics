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

    public boolean existsActiveDeduplicatedJob(UUID userId, String deduplicationKey) {
        Long count = em.createQuery(
                "select count(j) from BackgroundJob j " +
                "where j.user.id=:userId and j.deduplicationKey=:key " +
                "and j.status in (:queued, :waiting, :paused, :running)", Long.class)
            .setParameter("userId", userId)
            .setParameter("key", deduplicationKey)
            .setParameter("queued", BackgroundJobStatus.QUEUED)
            .setParameter("waiting", BackgroundJobStatus.WAITING)
            .setParameter("paused", BackgroundJobStatus.PAUSED_RATE_LIMIT)
            .setParameter("running", BackgroundJobStatus.RUNNING)
            .getSingleResult();
        return count != null && count > 0;
    }

    public boolean existsActiveRepositoryJobExcept(
            UUID userId, String jobType, UUID repositoryId, UUID excludedJobId) {
        String sql = "SELECT count(*) FROM background_job WHERE user_id=:userId AND job_type=:jobType " +
                "AND status IN ('QUEUED','WAITING','PAUSED_RATE_LIMIT','RUNNING') " +
                "AND payload->>'repositoryId'=:repositoryId" +
                (excludedJobId == null ? "" : " AND id<>:excludedJobId");
        var query = em.createNativeQuery(sql)
                .setParameter("userId", userId)
                .setParameter("jobType", jobType)
                .setParameter("repositoryId", repositoryId.toString());
        if (excludedJobId != null) query.setParameter("excludedJobId", excludedJobId);
        Number count = (Number) query.getSingleResult();
        return count != null && count.longValue() > 0;
    }

    public long countRunningJobsForUserByTypesExcept(
            UUID userId,
            Collection<String> jobTypes,
            UUID excludedJobId
    ) {
        if (userId == null || jobTypes == null || jobTypes.isEmpty()) return 0L;
        String jpql = "select count(j) from BackgroundJob j where j.user.id=:userId " +
                "and j.status=:running and j.jobType in :jobTypes" +
                (excludedJobId == null ? "" : " and j.id<>:excludedJobId");
        var query = em.createQuery(jpql, Long.class)
                .setParameter("userId", userId)
                .setParameter("running", BackgroundJobStatus.RUNNING)
                .setParameter("jobTypes", jobTypes);
        if (excludedJobId != null) query.setParameter("excludedJobId", excludedJobId);
        Long count = query.getSingleResult();
        return count == null ? 0L : count;
    }

    public List<BackgroundJob> findRecentForUser(UUID userId, int limit) {
        return em.createQuery(
                "select j from BackgroundJob j where j.user.id=:userId order by j.createdAt desc",
                BackgroundJob.class)
            .setParameter("userId", userId)
            .setMaxResults(Math.max(1, Math.min(limit, 200)))
            .getResultList();
    }

    public List<BackgroundJob> findActiveForUser(UUID userId) {
        return em.createQuery(
                "select j from BackgroundJob j where j.user.id=:userId " +
                "and j.status in (:queued, :waiting, :paused, :running) order by j.createdAt asc",
                BackgroundJob.class)
            .setParameter("userId", userId)
            .setParameter("queued", BackgroundJobStatus.QUEUED)
            .setParameter("waiting", BackgroundJobStatus.WAITING)
            .setParameter("paused", BackgroundJobStatus.PAUSED_RATE_LIMIT)
            .setParameter("running", BackgroundJobStatus.RUNNING)
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
            "SELECT id FROM background_job WHERE status IN ('QUEUED','WAITING','PAUSED_RATE_LIMIT') " +
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
    public int cancelProviderJobs(UUID userId, String provider, OffsetDateTime now) {
        return em.createNativeQuery(
                "UPDATE background_job SET status='CANCELLED', completed_at=:now, " +
                "locked_at=NULL, locked_by=NULL, last_error='Cancelled because provider was disconnected' " +
                "WHERE user_id=:userId AND status IN ('QUEUED','WAITING','PAUSED_RATE_LIMIT') " +
                "AND (payload->>'provider'=:provider OR deduplication_key LIKE :providerPrefix)")
            .setParameter("now", now)
            .setParameter("userId", userId)
            .setParameter("provider", provider)
            .setParameter("providerPrefix", provider + ":%")
            .executeUpdate();
    }

    @Transactional
    public int recoverStaleRunningJobs(OffsetDateTime lockedBefore, OffsetDateTime nextExecutionAt) {
        @SuppressWarnings("unchecked")
        List<UUID> ids = em.createNativeQuery(
                "SELECT id FROM background_job WHERE status='RUNNING' AND locked_at < :lockedBefore " +
                "FOR UPDATE SKIP LOCKED")
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
