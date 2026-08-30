package io.github.developeranalytics.persistence.auth;

import io.github.developeranalytics.domain.model.ProviderConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProviderConnectionRepository {

    @Inject
    EntityManager entityManager;

    public List<ProviderConnection> findAllForUser(UUID userId) {
        return entityManager.createQuery(
                "select c from ProviderConnection c " +
                "left join fetch c.providerIdentity " +
                "where c.user.id = :userId order by c.provider",
                ProviderConnection.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    public Optional<ProviderConnection> findForUserAndProvider(UUID userId, String provider) {
        return entityManager.createQuery(
                "select c from ProviderConnection c " +
                "left join fetch c.providerIdentity " +
                "where c.user.id = :userId and c.provider = :provider",
                ProviderConnection.class)
            .setParameter("userId", userId)
            .setParameter("provider", provider)
            .getResultStream()
            .findFirst();
    }

    @Transactional
    public void disconnect(UUID userId, String provider) {
        findForUserAndProvider(userId, provider).ifPresent(ProviderConnection::disconnect);
    }

    @Transactional
    public void markValidated(UUID userId, String provider) {
        findForUserAndProvider(userId, provider).ifPresent(ProviderConnection::markValidated);
    }
}
