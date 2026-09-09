package io.github.developeranalytics.service.account;

import io.github.developeranalytics.observability.StructuredLog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class UserDataDeletionService {

    private static final Logger LOG = Logger.getLogger(UserDataDeletionService.class);

    @Inject
    UserDataDeletionPersistenceService persistence;

    public DeletionResult deleteUser(UUID userId) {
        long started = System.nanoTime();
        StructuredLog.info(LOG, "data_deletion_started", StructuredLog.fields());

        try {
            DeletionResult result = persistence.deleteUser(userId);
            StructuredLog.info(LOG, "data_deletion_completed", StructuredLog.fields(
                    "durationMs", elapsedMillis(started),
                    "providerConnections", result.deletedDataCounts().get("providerConnections"),
                    "repositories", result.deletedDataCounts().get("repositories"),
                    "contributions", result.deletedDataCounts().get("contributions"),
                    "backgroundJobs", result.deletedDataCounts().get("backgroundJobs")
            ));
            return result;
        } catch (NotFoundException e) {
            throw e;
        } catch (RuntimeException e) {
            StructuredLog.warn(LOG, "data_deletion_failed", e, StructuredLog.fields(
                    "durationMs", elapsedMillis(started),
                    "rolledBack", true
            ));
            throw new DataDeletionFailedException(
                    "User data deletion could not be completed safely",
                    e
            );
        }
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }

    public record DeletionResult(
            UUID deletedUserId,
            Map<String, Long> deletedDataCounts,
            int persistedReportsDeleted
    ) {}
}
