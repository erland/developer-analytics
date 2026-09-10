package io.github.developeranalytics.persistence.repository;

import io.github.developeranalytics.domain.model.Contribution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ContributionRepository {

    @Inject
    EntityManager entityManager;

    public void persist(Contribution contribution) {
        entityManager.persist(contribution);
    }

    public int deleteForRepository(UUID userId, UUID repositoryId) {
        return entityManager.createQuery("delete from Contribution c where c.user.id=:userId and c.repository.id=:repositoryId")
                .setParameter("userId", userId).setParameter("repositoryId", repositoryId).executeUpdate();
    }

    public Optional<OffsetDateTime> latestCommitAt(UUID userId, UUID repositoryId) {
        return entityManager.createQuery(
                "select max(c.occurredAt) from Contribution c " +
                "where c.user.id=:userId and c.repository.id=:repositoryId and c.type=:type",
                OffsetDateTime.class)
                .setParameter("userId", userId)
                .setParameter("repositoryId", repositoryId)
                .setParameter("type", Contribution.Type.COMMIT)
                .getResultStream()
                .filter(java.util.Objects::nonNull)
                .findFirst();
    }

    public List<Contribution> findCommitsMissingFileClassification(
            UUID userId,
            UUID repositoryId,
            String classifierVersion,
            int limit
    ) {
        return entityManager.createQuery(
                "select c from Contribution c " +
                        "where c.user.id=:userId and c.repository.id=:repositoryId and c.type=:type " +
                        "and (c.additions is null or c.deletions is null or c.changedFiles is null " +
                        "or (c.changedFiles<>0 and (select count(f.id) from ContributionFileChange f " +
                        "where f.contribution=c and f.classifierVersion=:version) <> c.changedFiles)) " +
                        "order by c.occurredAt desc", Contribution.class)
                .setParameter("userId", userId)
                .setParameter("repositoryId", repositoryId)
                .setParameter("type", Contribution.Type.COMMIT)
                .setParameter("version", classifierVersion)
                .setMaxResults(Math.max(1, Math.min(limit, 1_000)))
                .getResultList();
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
