package io.github.developeranalytics.persistence.repository;

import io.github.developeranalytics.domain.change.ContributionFileChange;
import io.github.developeranalytics.domain.model.Contribution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

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
}
