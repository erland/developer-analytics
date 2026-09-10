package io.github.developeranalytics.persistence.repository;

import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.RepositorySyncStatus;
import io.github.developeranalytics.domain.model.SourceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class SourceRepositoryRepository {

    @Inject
    EntityManager entityManager;

    public void persist(SourceRepository repository) {
        entityManager.persist(repository);
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

    public List<SourceRepository> findContributionSyncCandidates(
            UUID userId,
            int offset,
            int limit
    ) {
        return entityManager.createQuery(
                "select r from SourceRepository r " +
                "where r.user.id=:userId " +
                "and r.includedInAnalysis = true " +
                "and r.syncStatus <> :accessRevoked " +
                "order by r.lastActivityAt desc nulls last, r.name",
                SourceRepository.class)
            .setParameter("userId", userId)
            .setParameter("accessRevoked", RepositorySyncStatus.ACCESS_REVOKED)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
    }

    /**
     * Returns repositories that need contribution-scope work. Besides schema/scope upgrades,
     * this deliberately reconciles repositories already marked current when a previous provider
     * failure left commit line statistics or change-kind classification incomplete. Existing
     * contribution rows are preserved; the backfill job only enriches missing commit details.
     */
    public List<SourceRepository> findContributionScopeUpgradeCandidates(int limit, String classifierVersion) {
        return entityManager.createQuery(
                "select r from SourceRepository r " +
                "where r.provider=:provider " +
                "and r.includedInAnalysis = true " +
                "and r.syncStatus <> :accessRevoked " +
                "and (r.contributionScopeVersion < :currentVersion " +
                "or exists (select c.id from Contribution c " +
                "where c.repository=r and c.type=:commitType " +
                "and (c.additions is null or c.deletions is null or c.changedFiles is null " +
                "or (c.changedFiles<>0 and (select count(f.id) from ContributionFileChange f " +
                "where f.contribution=c and f.classifierVersion=:classifierVersion) <> c.changedFiles)))) " +
                "order by r.lastActivityAt desc nulls last, r.name",
                SourceRepository.class)
            .setParameter("provider", "github")
            .setParameter("accessRevoked", RepositorySyncStatus.ACCESS_REVOKED)
            .setParameter("currentVersion", SourceRepository.CURRENT_CONTRIBUTION_SCOPE_VERSION)
            .setParameter("commitType", Contribution.Type.COMMIT)
            .setParameter("classifierVersion", classifierVersion)
            .setMaxResults(Math.max(1, Math.min(limit, 500)))
            .getResultList();
    }

    public List<SourceRepository> findAnalysisCandidates(UUID userId) {
        return entityManager.createQuery(
                "select r from SourceRepository r " +
                "where r.user.id=:userId " +
                "and r.includedInAnalysis = true " +
                "and r.syncStatus <> :accessRevoked " +
                "order by r.lastActivityAt desc nulls last, r.name",
                SourceRepository.class)
            .setParameter("userId", userId)
            .setParameter("accessRevoked", RepositorySyncStatus.ACCESS_REVOKED)
            .getResultList();
    }

    public List<SourceRepository> findByUser(UUID userId) {
        return findAllForUser(userId);
    }

    public List<SourceRepository> findAllForUser(UUID userId) {
        return entityManager.createQuery(
                "select r from SourceRepository r " +
                "where r.user.id = :userId " +
                "order by r.lastActivityAt desc nulls last, r.name",
                SourceRepository.class)
            .setParameter("userId", userId)
            .getResultList();
    }

    public List<SourceRepository> findPrivateForUser(UUID userId) {
        return entityManager.createQuery(
                "select r from SourceRepository r " +
                "where r.user.id=:userId and r.visibility=:visibility " +
                "order by r.name",
                SourceRepository.class)
            .setParameter("userId", userId)
            .setParameter(
                    "visibility",
                    io.github.developeranalytics.domain.model.RepositoryVisibility.PRIVATE
            )
            .getResultList();
    }

    public Optional<SourceRepository> findByIdForUser(UUID repositoryId, UUID userId) {
        return entityManager.createQuery(
                "select r from SourceRepository r " +
                "where r.id = :repositoryId and r.user.id = :userId",
                SourceRepository.class)
            .setParameter("repositoryId", repositoryId)
            .setParameter("userId", userId)
            .getResultStream()
            .findFirst();
    }
}
