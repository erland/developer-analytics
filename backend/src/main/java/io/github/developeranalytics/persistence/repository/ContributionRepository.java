package io.github.developeranalytics.persistence.repository;

import io.github.developeranalytics.domain.model.Contribution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ContributionRepository {

    @Inject
    EntityManager entityManager;

    public void persist(Contribution contribution) {
        entityManager.persist(contribution);
    }

    public Optional<Contribution> findByProviderIdentity(
            UUID userId,
            String provider,
            String externalContributionId,
            Contribution.Type type
    ) {
        return entityManager.createQuery(
                "select c from Contribution c " +
                "where c.user.id=:userId and c.provider=:provider " +
                "and c.providerContributionId=:externalId and c.type=:type",
                Contribution.class)
            .setParameter("userId", userId)
            .setParameter("provider", provider)
            .setParameter("externalId", externalContributionId)
            .setParameter("type", type)
            .getResultStream()
            .findFirst();
    }
}
