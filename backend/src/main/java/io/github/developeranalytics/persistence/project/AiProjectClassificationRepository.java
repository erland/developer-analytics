package io.github.developeranalytics.persistence.project;

import io.github.developeranalytics.domain.project.AiProjectClassification;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AiProjectClassificationRepository {

    @Inject
    EntityManager entityManager;

    public void persist(AiProjectClassification classification) {
        entityManager.persist(classification);
    }

    public Optional<AiProjectClassification> findReusable(
            UUID repositoryId,
            String fingerprint,
            String analysisVersion,
            String providerId,
            String modelId
    ) {
        return entityManager.createQuery(
                "select a from AiProjectClassification a " +
                "where a.repository.id=:repositoryId " +
                "and a.inputFingerprint=:fingerprint " +
                "and a.analysisVersion=:analysisVersion " +
                "and a.providerId=:providerId and a.modelId=:modelId " +
                "order by a.createdAt desc",
                AiProjectClassification.class
        )
        .setParameter("repositoryId", repositoryId)
        .setParameter("fingerprint", fingerprint)
        .setParameter("analysisVersion", analysisVersion)
        .setParameter("providerId", providerId)
        .setParameter("modelId", modelId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
    }

    public Optional<AiProjectClassification> latest(UUID repositoryId) {
        return entityManager.createQuery(
                "select a from AiProjectClassification a " +
                "where a.repository.id=:repositoryId order by a.createdAt desc",
                AiProjectClassification.class
        )
        .setParameter("repositoryId", repositoryId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
    }
}
