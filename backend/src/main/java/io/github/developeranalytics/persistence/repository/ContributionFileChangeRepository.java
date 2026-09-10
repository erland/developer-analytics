package io.github.developeranalytics.persistence.repository;

import io.github.developeranalytics.domain.change.ContributionFileChange;
import io.github.developeranalytics.domain.model.Contribution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.UUID;

@ApplicationScoped
public class ContributionFileChangeRepository {

    @Inject
    EntityManager entityManager;

    public void persist(ContributionFileChange change) {
        entityManager.persist(change);
    }

    public int deleteForContribution(Contribution contribution) {
        return entityManager.createQuery(
                "delete from ContributionFileChange f where f.contribution=:contribution")
                .setParameter("contribution", contribution)
                .executeUpdate();
    }

    public boolean hasCurrentClassification(Contribution contribution, String classifierVersion) {
        if (contribution.getAdditions() == null
                || contribution.getDeletions() == null
                || contribution.getChangedFiles() == null) {
            return false;
        }
        if (Integer.valueOf(0).equals(contribution.getChangedFiles())) return true;
        Long count = entityManager.createQuery(
                "select count(f.id) from ContributionFileChange f " +
                        "where f.contribution=:contribution and f.classifierVersion=:version", Long.class)
                .setParameter("contribution", contribution)
                .setParameter("version", classifierVersion)
                .getSingleResult();
        return count != null && count > 0;
    }

    public boolean hasMissingCurrentClassification(UUID userId, UUID repositoryId, String classifierVersion) {
        Long count = entityManager.createQuery(
                "select count(c.id) from Contribution c " +
                        "where c.user.id=:userId and c.repository.id=:repositoryId and c.type=:type " +
                        "and (c.additions is null or c.deletions is null or c.changedFiles is null " +
                        "or (c.changedFiles<>0 and not exists (select f.id from ContributionFileChange f " +
                        "where f.contribution=c and f.classifierVersion=:version)))", Long.class)
                .setParameter("userId", userId)
                .setParameter("repositoryId", repositoryId)
                .setParameter("type", Contribution.Type.COMMIT)
                .setParameter("version", classifierVersion)
                .getSingleResult();
        return count != null && count > 0;
    }
}
