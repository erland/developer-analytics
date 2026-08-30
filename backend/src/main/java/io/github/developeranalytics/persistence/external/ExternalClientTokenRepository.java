package io.github.developeranalytics.persistence.external;

import io.github.developeranalytics.domain.external.ExternalClientToken;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ExternalClientTokenRepository {

    @Inject
    EntityManager entityManager;

    public void persist(ExternalClientToken token) {
        entityManager.persist(token);
    }

    public Optional<ExternalClientToken> findActiveByHash(String tokenHash) {
        return entityManager.createQuery(
                "select t from ExternalClientToken t join fetch t.user " +
                "where t.tokenHash=:tokenHash and t.revokedAt is null",
                ExternalClientToken.class
        )
        .setParameter("tokenHash", tokenHash)
        .getResultStream()
        .findFirst();
    }

    public List<ExternalClientToken> findForUser(UUID userId) {
        return entityManager.createQuery(
                "select t from ExternalClientToken t " +
                "where t.user.id=:userId order by t.createdAt desc",
                ExternalClientToken.class
        )
        .setParameter("userId", userId)
        .getResultList();
    }

    public Optional<ExternalClientToken> findByIdForUser(
            UUID tokenId,
            UUID userId
    ) {
        return entityManager.createQuery(
                "select t from ExternalClientToken t " +
                "where t.id=:tokenId and t.user.id=:userId",
                ExternalClientToken.class
        )
        .setParameter("tokenId", tokenId)
        .setParameter("userId", userId)
        .getResultStream()
        .findFirst();
    }
}
