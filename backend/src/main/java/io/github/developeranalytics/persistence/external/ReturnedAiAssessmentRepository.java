package io.github.developeranalytics.persistence.external;

import io.github.developeranalytics.domain.external.ReturnedAiAssessment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ReturnedAiAssessmentRepository {

    @Inject
    EntityManager entityManager;

    public void persist(ReturnedAiAssessment assessment) {
        entityManager.persist(assessment);
    }

    public List<ReturnedAiAssessment> findForUser(
            UUID userId,
            int limit
    ) {
        return entityManager.createQuery(
                "select a from ReturnedAiAssessment a " +
                "where a.user.id=:userId order by a.createdAt desc",
                ReturnedAiAssessment.class
        )
        .setParameter("userId", userId)
        .setMaxResults(limit)
        .getResultList();
    }

    public Optional<ReturnedAiAssessment> findByIdForUser(
            UUID assessmentId,
            UUID userId
    ) {
        return entityManager.createQuery(
                "select a from ReturnedAiAssessment a " +
                "where a.id=:assessmentId and a.user.id=:userId",
                ReturnedAiAssessment.class
        )
        .setParameter("assessmentId", assessmentId)
        .setParameter("userId", userId)
        .getResultStream()
        .findFirst();
    }

    public void delete(ReturnedAiAssessment assessment) {
        entityManager.remove(assessment);
    }
}
