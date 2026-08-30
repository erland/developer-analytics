package io.github.developeranalytics.persistence.technology;

import io.github.developeranalytics.domain.technology.UserTechnologyAssessment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserTechnologyAssessmentRepository {

    @Inject
    EntityManager entityManager;

    public void persist(UserTechnologyAssessment assessment) {
        entityManager.persist(assessment);
    }

    public Optional<UserTechnologyAssessment> find(
            UUID userId,
            UUID technologyId
    ) {
        return entityManager.createQuery(
                "select a from UserTechnologyAssessment a " +
                "where a.user.id=:userId and a.technology.id=:technologyId",
                UserTechnologyAssessment.class)
            .setParameter("userId", userId)
            .setParameter("technologyId", technologyId)
            .getResultStream()
            .findFirst();
    }

    public List<UserTechnologyAssessment> findForUser(UUID userId) {
        return entityManager.createQuery(
                "select a from UserTechnologyAssessment a " +
                "join fetch a.technology " +
                "where a.user.id=:userId " +
                "order by a.score desc, a.technology.displayName",
                UserTechnologyAssessment.class)
            .setParameter("userId", userId)
            .getResultList();
    }
}
