package io.github.developeranalytics.persistence.insight;

import io.github.developeranalytics.domain.insight.UserAiInsight;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserAiInsightRepository {

    @Inject
    EntityManager entityManager;

    public void persist(UserAiInsight insight) {
        entityManager.persist(insight);
    }

    public Optional<UserAiInsight> findReusable(
            UUID userId,
            String fingerprint,
            String analysisVersion,
            String providerId,
            String modelId
    ) {
        return entityManager.createQuery(
                "select i from UserAiInsight i " +
                "where i.user.id=:userId " +
                "and i.inputFingerprint=:fingerprint " +
                "and i.analysisVersion=:analysisVersion " +
                "and i.providerId=:providerId " +
                "and i.modelId=:modelId order by i.createdAt desc",
                UserAiInsight.class
        )
        .setParameter("userId", userId)
        .setParameter("fingerprint", fingerprint)
        .setParameter("analysisVersion", analysisVersion)
        .setParameter("providerId", providerId)
        .setParameter("modelId", modelId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
    }

    public Optional<UserAiInsight> latest(UUID userId) {
        return entityManager.createQuery(
                "select i from UserAiInsight i " +
                "where i.user.id=:userId order by i.createdAt desc",
                UserAiInsight.class
        )
        .setParameter("userId", userId)
        .setMaxResults(1)
        .getResultStream()
        .findFirst();
    }
}
