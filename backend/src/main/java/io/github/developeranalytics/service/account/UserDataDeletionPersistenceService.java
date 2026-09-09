package io.github.developeranalytics.service.account;

import io.github.developeranalytics.observability.StructuredLog;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Executes the destructive database work in one short, isolated transaction. */
@ApplicationScoped
public class UserDataDeletionPersistenceService {

    private static final Logger LOG = Logger.getLogger(UserDataDeletionPersistenceService.class);

    @Inject
    EntityManager entityManager;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public UserDataDeletionService.DeletionResult deleteUser(UUID userId) {
        long countsStarted = System.nanoTime();
        Map<String, Long> before = new LinkedHashMap<>();

        before.put("providerConnections", count(
                "select count(c.id) from ProviderConnection c where c.user.id=:userId",
                userId
        ));
        before.put("repositories", count(
                "select count(r.id) from SourceRepository r where r.user.id=:userId",
                userId
        ));
        before.put("contributions", count(
                "select count(c.id) from Contribution c where c.user.id=:userId",
                userId
        ));
        before.put("backgroundJobs", count(
                "select count(j.id) from BackgroundJob j where j.user.id=:userId",
                userId
        ));
        before.put("technologyEvidence", count(
                "select count(e.id) from RepositoryTechnologyEvidence e where e.user.id=:userId",
                userId
        ));
        before.put("technologyAssessments", count(
                "select count(a.id) from UserTechnologyAssessment a where a.user.id=:userId",
                userId
        ));
        before.put("projectAssessments", count(
                "select count(a.id) from ProjectSignificanceAssessment a where a.user.id=:userId",
                userId
        ));
        before.put("aiAssessments", count(
                "select count(a.id) from UserAiInsight a where a.user.id=:userId",
                userId
        ) + count(
                "select count(a.id) from ReturnedAiAssessment a where a.user.id=:userId",
                userId
        ));
        long countsDurationMs = elapsedMillis(countsStarted);

        long deleteStarted = System.nanoTime();
        int deletedUsers = entityManager.createNativeQuery(
                "DELETE FROM app_user WHERE id=:userId"
        )
        .setParameter("userId", userId)
        .executeUpdate();

        if (deletedUsers != 1) {
            throw new NotFoundException("User account no longer exists");
        }

        entityManager.flush();
        long deleteDurationMs = elapsedMillis(deleteStarted);

        StructuredLog.info(LOG, "data_deletion_database_phases", StructuredLog.fields(
                "countsDurationMs", countsDurationMs,
                "cascadeDeleteDurationMs", deleteDurationMs,
                "repositories", before.get("repositories"),
                "contributions", before.get("contributions")
        ));

        return new UserDataDeletionService.DeletionResult(
                userId,
                Map.copyOf(before),
                0
        );
    }

    private long count(String jpql, UUID userId) {
        return entityManager.createQuery(jpql, Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    private long elapsedMillis(long started) {
        return (System.nanoTime() - started) / 1_000_000L;
    }
}
