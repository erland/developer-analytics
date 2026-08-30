package io.github.developeranalytics.persistence.correction;

import io.github.developeranalytics.domain.correction.UserAnalysisCorrection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserAnalysisCorrectionRepository {

    @Inject
    EntityManager entityManager;

    public void persist(UserAnalysisCorrection correction) {
        entityManager.persist(correction);
    }

    public Optional<UserAnalysisCorrection> find(
            UUID userId,
            UUID repositoryId,
            UserAnalysisCorrection.Type type,
            String correctionKey
    ) {
        return entityManager.createQuery(
                "select c from UserAnalysisCorrection c " +
                "where c.user.id=:userId " +
                "and ((:repositoryId is null and c.repository is null) " +
                "or c.repository.id=:repositoryId) " +
                "and c.type=:type " +
                "and ((:correctionKey is null and c.correctionKey is null) " +
                "or c.correctionKey=:correctionKey)",
                UserAnalysisCorrection.class
        )
        .setParameter("userId", userId)
        .setParameter("repositoryId", repositoryId)
        .setParameter("type", type)
        .setParameter("correctionKey", normalize(correctionKey))
        .getResultStream()
        .findFirst();
    }

    public List<UserAnalysisCorrection> findForUser(UUID userId) {
        return entityManager.createQuery(
                "select c from UserAnalysisCorrection c " +
                "left join fetch c.repository " +
                "where c.user.id=:userId order by c.createdAt desc",
                UserAnalysisCorrection.class
        )
        .setParameter("userId", userId)
        .getResultList();
    }

    public List<UserAnalysisCorrection> findForRepository(
            UUID userId,
            UUID repositoryId
    ) {
        return entityManager.createQuery(
                "select c from UserAnalysisCorrection c " +
                "where c.user.id=:userId and c.repository.id=:repositoryId",
                UserAnalysisCorrection.class
        )
        .setParameter("userId", userId)
        .setParameter("repositoryId", repositoryId)
        .getResultList();
    }

    public void delete(UserAnalysisCorrection correction) {
        entityManager.remove(correction);
    }

    public boolean exists(
            UUID userId,
            UUID repositoryId,
            UserAnalysisCorrection.Type type,
            String correctionKey
    ) {
        return find(userId, repositoryId, type, correctionKey).isPresent();
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toLowerCase();
    }
}
