package io.github.developeranalytics.service.account;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class UserDataDeletionService {

    @Inject
    EntityManager entityManager;

    @Transactional
    public DeletionResult deleteUser(UUID userId) {
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

        int deletedUsers = entityManager.createNativeQuery(
                "DELETE FROM app_user WHERE id=:userId"
        )
        .setParameter("userId", userId)
        .executeUpdate();

        if (deletedUsers != 1) {
            throw new NotFoundException("User account no longer exists");
        }

        entityManager.flush();

        return new DeletionResult(
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

    public record DeletionResult(
            UUID deletedUserId,
            Map<String, Long> deletedDataCounts,
            int persistedReportsDeleted
    ) {}
}
