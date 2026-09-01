package io.github.developeranalytics.persistence.project;

import io.github.developeranalytics.domain.project.ProjectSignificanceAssessment;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProjectSignificanceRepository {

    @Inject
    EntityManager entityManager;

    public void persist(ProjectSignificanceAssessment assessment) {
        entityManager.persist(assessment);
    }

    public Optional<ProjectSignificanceAssessment> find(
            UUID userId,
            UUID repositoryId
    ) {
        return entityManager.createQuery(
                "select a from ProjectSignificanceAssessment a " +
                "where a.user.id=:userId and a.repository.id=:repositoryId",
                ProjectSignificanceAssessment.class)
            .setParameter("userId", userId)
            .setParameter("repositoryId", repositoryId)
            .getResultStream()
            .findFirst();
    }

    public List<ProjectSignificanceAssessment> findRanked(UUID userId) {
        return entityManager.createQuery(
                "select a from ProjectSignificanceAssessment a " +
                "join fetch a.repository " +
                "where a.user.id=:userId " +
                "order by a.significanceScore desc, " +
                "a.involvementScore desc, a.repository.name",
                ProjectSignificanceAssessment.class)
            .setParameter("userId", userId)
            .getResultList();
    }


public List<ProjectSignificanceAssessment> findSignificantExternalProjects(
        UUID userId
) {
    return entityManager.createQuery(
            "select a from ProjectSignificanceAssessment a " +
            "join fetch a.repository r " +
            "where a.user.id=:userId and r.includedInAnalysis=true " +
            "and r.ownershipRelation <> :ownedByUser " +
            "and (" +
            "a.significanceLevel in (:high, :veryHigh) " +
            "or a.involvementLevel in (:high, :veryHigh)" +
            ") " +
            "order by " +
            "case " +
            "when a.significanceLevel=:veryHigh and a.involvementLevel=:veryHigh then 0 " +
            "when a.significanceLevel in (:high, :veryHigh) " +
            "     and a.involvementLevel in (:high, :veryHigh) then 1 " +
            "when a.significanceLevel in (:high, :veryHigh) then 2 " +
            "else 3 end, " +
            "a.significanceScore desc, " +
            "a.involvementScore desc, " +
            "r.name",
            ProjectSignificanceAssessment.class)
        .setParameter(
                "ownedByUser",
                io.github.developeranalytics.domain.model.RepositoryOwnershipRelation.OWNED_BY_USER
        )
        .setParameter("high", ProjectSignificanceAssessment.Level.HIGH)
        .setParameter("veryHigh", ProjectSignificanceAssessment.Level.VERY_HIGH)
        .setParameter("userId", userId)
        .getResultList();
}

    public ProjectMetrics metrics(UUID userId, UUID repositoryId) {
        Object[] row = entityManager.createQuery(
                "select count(c.id), min(c.occurredAt), max(c.occurredAt), " +
                "sum(case when c.occurredAt >= :recentThreshold then 1 else 0 end) " +
                "from Contribution c " +
                "where c.user.id=:userId and c.repository.id=:repositoryId",
                Object[].class)
            .setParameter("userId", userId)
            .setParameter("repositoryId", repositoryId)
            .setParameter(
                    "recentThreshold",
                    OffsetDateTime.now(java.time.ZoneOffset.UTC).minusMonths(12)
            )
            .getSingleResult();

        long contributions = ((Number) row[0]).longValue();
        OffsetDateTime first = (OffsetDateTime) row[1];
        OffsetDateTime last = (OffsetDateTime) row[2];
        long recent = ((Number) row[3]).longValue();

        Object[] repoRow = entityManager.createQuery(
                "select r.discoveredAt, r.lastActivityAt, r.ownershipRelation, " +
                "r.ownerType, r.fork, r.archived, r.repositoryCommitCount " +
                "from SourceRepository r " +
                "where r.id=:repositoryId and r.user.id=:userId",
                Object[].class)
            .setParameter("repositoryId", repositoryId)
            .setParameter("userId", userId)
            .getSingleResult();

        OffsetDateTime discovered = (OffsetDateTime) repoRow[0];
        OffsetDateTime lastActivity = (OffsetDateTime) repoRow[1];
        Object ownershipRelation = repoRow[2];
        Object ownerType = repoRow[3];
        boolean fork = (Boolean) repoRow[4];
        boolean archived = (Boolean) repoRow[5];
        Integer repositoryCommitCount = (Integer) repoRow[6];

        long projectContributionCount = repositoryCommitCount == null ? contributions : repositoryCommitCount.longValue();

        long categoryCount = entityManager.createQuery(
                "select count(c.category.id) from RepositoryProjectCategory c " +
                "where c.repository.id=:repositoryId",
                Long.class)
            .setParameter("repositoryId", repositoryId)
            .getSingleResult();

        return new ProjectMetrics(
                contributions,
                first,
                last,
                recent,
                discovered,
                lastActivity,
                ownershipRelation == null ? null : ownershipRelation.toString(),
                ownerType == null ? null : ownerType.toString(),
                fork,
                archived,
                projectContributionCount,
                categoryCount
        );
    }

    public record ProjectMetrics(
            long userContributionCount,
            OffsetDateTime firstUserContributionAt,
            OffsetDateTime lastUserContributionAt,
            long recentUserContributionCount,
            OffsetDateTime discoveredAt,
            OffsetDateTime lastActivityAt,
            String ownershipRelation,
            String ownerType,
            boolean fork,
            boolean archived,
            long totalObservedContributionCount,
            long categoryCount
    ) {}
}
