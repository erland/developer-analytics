package io.github.developeranalytics.worker;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@ApplicationScoped
public class WorkerHeartbeatService {

    @Inject EntityManager entityManager;

    @ConfigProperty(
            name="developer-analytics.runtime-role",
            defaultValue="api"
    )
    String runtimeRole;

    @ConfigProperty(
            name="developer-analytics.worker.id",
            defaultValue="worker-1"
    )
    String workerId;

    @Scheduled(
            every="30s",
            concurrentExecution=Scheduled.ConcurrentExecution.SKIP
    )
    @Transactional
    void heartbeat() {
        if (!"worker".equalsIgnoreCase(runtimeRole)) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        entityManager.createNativeQuery(
                "INSERT INTO worker_heartbeat(" +
                "worker_id,runtime_role,last_seen_at,updated_at) " +
                "VALUES(:workerId,:role,:now,:now) " +
                "ON CONFLICT(worker_id) DO UPDATE SET " +
                "runtime_role=EXCLUDED.runtime_role," +
                "last_seen_at=EXCLUDED.last_seen_at," +
                "updated_at=EXCLUDED.updated_at"
        )
        .setParameter("workerId", workerId)
        .setParameter("role", runtimeRole)
        .setParameter("now", now)
        .executeUpdate();
    }

    public Optional<WorkerStatus> latestStatus() {
        try {
            Object[] row = (Object[]) entityManager.createNativeQuery(
                    "SELECT worker_id,last_seen_at FROM worker_heartbeat " +
                    "ORDER BY last_seen_at DESC LIMIT 1"
            ).getResultStream().findFirst().orElse(null);

            if (row == null) {
                return Optional.empty();
            }

            OffsetDateTime lastSeen = (OffsetDateTime) row[1];
            boolean available = lastSeen.isAfter(
                    OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(2)
            );

            return Optional.of(new WorkerStatus(
                    row[0].toString(),
                    lastSeen,
                    available
            ));
        } catch (RuntimeException unavailableBeforeMigration) {
            return Optional.empty();
        }
    }

    public record WorkerStatus(
            String workerId,
            OffsetDateTime lastSeenAt,
            boolean available
    ) {}
}
