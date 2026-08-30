package io.github.developeranalytics.service.connection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.UUID;

@ApplicationScoped
public class ProviderDisconnectDataService {

    @Inject EntityManager entityManager;

    public RemovalSummary removeAnalysedProviderData(
            UUID userId,
            String provider
    ) {
        int returnedAiAssessments = entityManager.createQuery(
                "delete from ReturnedAiAssessment a where a.user.id=:userId"
        ).setParameter("userId", userId).executeUpdate();

        int userAiInsights = entityManager.createQuery(
                "delete from UserAiInsight a where a.user.id=:userId"
        ).setParameter("userId", userId).executeUpdate();

        int corrections = entityManager.createQuery(
                "delete from UserAnalysisCorrection c where c.user.id=:userId"
        ).setParameter("userId", userId).executeUpdate();

        int technologyAssessments = entityManager.createQuery(
                "delete from UserTechnologyAssessment a where a.user.id=:userId"
        ).setParameter("userId", userId).executeUpdate();

        int technologyMonths = entityManager.createQuery(
                "delete from TechnologyActivityMonth a where a.user.id=:userId"
        ).setParameter("userId", userId).executeUpdate();

        int userMonths = entityManager.createQuery(
                "delete from UserActivityMonth a where a.user.id=:userId"
        ).setParameter("userId", userId).executeUpdate();

        int repositorySyncRuns = entityManager.createQuery(
                "delete from RepositorySyncRun r " +
                "where r.user.id=:userId and r.provider=:provider"
        )
        .setParameter("userId", userId)
        .setParameter("provider", provider)
        .executeUpdate();

        // Repository-owned contributions, evidence, classifications,
        // project assessments, repository activity and contribution sync runs
        // cascade from source_repository.
        int repositories = entityManager.createQuery(
                "delete from SourceRepository r " +
                "where r.user.id=:userId and r.provider=:provider"
        )
        .setParameter("userId", userId)
        .setParameter("provider", provider)
        .executeUpdate();

        entityManager.flush();

        return new RemovalSummary(
                repositories,
                repositorySyncRuns,
                userMonths,
                technologyMonths,
                technologyAssessments,
                corrections,
                userAiInsights,
                returnedAiAssessments
        );
    }

    public record RemovalSummary(
            int repositories,
            int repositorySyncRuns,
            int userActivityMonths,
            int technologyActivityMonths,
            int technologyAssessments,
            int corrections,
            int userAiInsights,
            int returnedAiAssessments
    ) {}
}
