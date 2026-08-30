package io.github.developeranalytics.persistence.technology;

import io.github.developeranalytics.domain.technology.RepositoryTechnologyEvidence;
import io.github.developeranalytics.domain.technology.TechnologyEvidenceType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RepositoryTechnologyEvidenceRepository {

    @Inject
    EntityManager entityManager;

    public void persist(RepositoryTechnologyEvidence evidence) {
        entityManager.persist(evidence);
    }

    public Optional<RepositoryTechnologyEvidence> find(
            UUID repositoryId,
            UUID technologyId,
            TechnologyEvidenceType type,
            String sourceValue
    ) {
        return entityManager.createQuery(
                "select e from RepositoryTechnologyEvidence e " +
                "where e.repository.id=:repositoryId " +
                "and e.technology.id=:technologyId " +
                "and e.evidenceType=:type " +
                "and e.sourceValue=:sourceValue",
                RepositoryTechnologyEvidence.class)
            .setParameter("repositoryId", repositoryId)
            .setParameter("technologyId", technologyId)
            .setParameter("type", type)
            .setParameter("sourceValue", sourceValue)
            .getResultStream()
            .findFirst();
    }

    public List<RepositoryTechnologyEvidence> findForRepository(
            UUID userId,
            UUID repositoryId
    ) {
        return entityManager.createQuery(
                "select e from RepositoryTechnologyEvidence e " +
                "join fetch e.technology " +
                "where e.user.id=:userId and e.repository.id=:repositoryId " +
                "order by e.technology.displayName",
                RepositoryTechnologyEvidence.class)
            .setParameter("userId", userId)
            .setParameter("repositoryId", repositoryId)
            .getResultList();
    }

public List<TechnologyEvidenceSummaryRow> summarizeForUser(
        UUID userId,
        java.time.OffsetDateTime recentThreshold
) {
    @SuppressWarnings("unchecked")
    List<Object[]> rows = entityManager.createQuery(
            "select e.technology.id, " +
            "count(distinct e.repository.id), " +
            "count(e.id), " +
            "count(distinct e.evidenceType), " +
            "min(e.observedAt), " +
            "max(e.observedAt), " +
            "sum(case when e.repository.lastActivityAt >= :recentThreshold then 1 else 0 end) " +
            "from RepositoryTechnologyEvidence e " +
            "where e.user.id=:userId " +
            "group by e.technology.id",
            Object[].class)
        .setParameter("userId", userId)
        .setParameter("recentThreshold", recentThreshold)
        .getResultList();

    return rows.stream()
            .map(row -> new TechnologyEvidenceSummaryRow(
                    (UUID) row[0],
                    ((Number) row[1]).intValue(),
                    ((Number) row[2]).intValue(),
                    ((Number) row[3]).intValue(),
                    (java.time.OffsetDateTime) row[4],
                    (java.time.OffsetDateTime) row[5],
                    ((Number) row[6]).intValue()
            ))
            .toList();
}


public List<RepresentativeProjectRow> findRepresentativeProjects(
        UUID userId,
        UUID technologyId,
        int limit
) {
    return entityManager.createQuery(
            "select e.repository.id, e.repository.name, e.repository.htmlUrl, " +
            "e.repository.visibility, e.repository.ownershipRelation, " +
            "max(e.repository.lastActivityAt), count(e.id) " +
            "from RepositoryTechnologyEvidence e " +
            "where e.user.id=:userId and e.technology.id=:technologyId " +
            "group by e.repository.id, e.repository.name, e.repository.htmlUrl, " +
            "e.repository.visibility, e.repository.ownershipRelation " +
            "order by max(e.repository.lastActivityAt) desc nulls last, count(e.id) desc",
            Object[].class)
        .setParameter("userId", userId)
        .setParameter("technologyId", technologyId)
        .setMaxResults(Math.max(1, Math.min(limit, 10)))
        .getResultList()
        .stream()
        .map(row -> new RepresentativeProjectRow(
                (UUID) row[0],
                (String) row[1],
                (String) row[2],
                row[3].toString(),
                row[4].toString(),
                (java.time.OffsetDateTime) row[5],
                ((Number) row[6]).intValue()
        ))
        .toList();
}

public record RepresentativeProjectRow(
        UUID repositoryId,
        String repositoryName,
        String htmlUrl,
        String visibility,
        String ownershipRelation,
        java.time.OffsetDateTime lastActivityAt,
        int evidenceCount
) {}

public record TechnologyEvidenceSummaryRow(
        UUID technologyId,
        int repositoryCount,
        int evidenceCount,
        int independentEvidenceTypes,
        java.time.OffsetDateTime firstObservedAt,
        java.time.OffsetDateTime lastObservedAt,
        int recentRepositoryCount
) {}

}
