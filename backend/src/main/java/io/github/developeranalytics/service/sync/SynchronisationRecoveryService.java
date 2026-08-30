package io.github.developeranalytics.service.sync;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.RepositorySyncStatus;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@ApplicationScoped
public class SynchronisationRecoveryService {

    private static final long STALE_JOB_MINUTES = 10;

    @Inject BackgroundJobRepository jobs;
    @Inject EntityManager entityManager;

    @Transactional
    public int recoverInterruptedJobs() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return jobs.recoverStaleRunningJobs(
                now.minusMinutes(STALE_JOB_MINUTES),
                now
        );
    }

    @Transactional
    public void markProviderAccessLost(
            BackgroundJob job,
            Throwable failure
    ) {
        if (job.getUser() == null) {
            return;
        }

        String provider = provider(job);
        if (!"github".equals(provider)) {
            return;
        }

        UUID userId = job.getUser().getId();

        entityManager.createQuery(
                "update SourceRepository r set r.syncStatus=:status " +
                "where r.user.id=:userId and r.provider=:provider"
        )
        .setParameter("status", RepositorySyncStatus.ACCESS_REVOKED)
        .setParameter("userId", userId)
        .setParameter("provider", provider)
        .executeUpdate();

        entityManager.createQuery(
                "update ProviderConnection c set c.status=:status " +
                "where c.user.id=:userId and c.provider=:provider"
        )
        .setParameter(
                "status",
                io.github.developeranalytics.domain.model.ProviderConnection
                        .Status.ERROR.name()
        )
        .setParameter("userId", userId)
        .setParameter("provider", provider)
        .executeUpdate();

        jobs.cancelProviderJobs(
                userId,
                provider,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
    }

    private String provider(BackgroundJob job) {
        if (job.getPayload() != null &&
                job.getPayload().get("provider") != null) {
            return job.getPayload().get("provider")
                    .toString()
                    .toLowerCase();
        }

        String type = job.getJobType();
        return type != null && type.startsWith("GITHUB_")
                ? "github"
                : "";
    }
}
