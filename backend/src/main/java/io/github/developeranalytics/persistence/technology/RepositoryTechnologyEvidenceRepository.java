package io.github.developeranalytics.persistence.technology;

import io.github.developeranalytics.domain.model.RepositoryVisibility;
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

    @Inject EntityManager entityManager;

    public void persist(RepositoryTechnologyEvidence evidence) { entityManager.persist(evidence); }

    public Optional<RepositoryTechnologyEvidence> find(UUID repositoryId, UUID technologyId,
                                                        TechnologyEvidenceType type, String sourceValue) {
        return entityManager.createQuery(
                "select e from RepositoryTechnologyEvidence e where e.repository.id=:repositoryId " +
                "and e.technology.id=:technologyId and e.evidenceType=:type and e.sourceValue=:sourceValue",
                RepositoryTechnologyEvidence.class)
            .setParameter("repositoryId", repositoryId).setParameter("technologyId", technologyId)
            .setParameter("type", type).setParameter("sourceValue", sourceValue)
            .getResultStream().findFirst();
    }

    public List<RepositoryTechnologyEvidence> findForRepository(UUID userId, UUID repositoryId) {
        return entityManager.createQuery(
                "select e from RepositoryTechnologyEvidence e join fetch e.technology " +
                "where e.user.id=:userId and e.repository.id=:repositoryId order by e.technology.displayName",
                RepositoryTechnologyEvidence.class)
            .setParameter("userId", userId).setParameter("repositoryId", repositoryId).getResultList();
    }

    public List<TechnologyEvidenceSummaryRow> summarizeForUser(UUID userId,
                                                                java.time.OffsetDateTime recentThreshold) {
        List<Object[]> rows = entityManager.createQuery(
                "select e.technology.id, count(distinct e.repository.id), count(e.id), " +
                "count(distinct e.evidenceType), min(e.observedAt), max(e.observedAt), " +
                "sum(case when e.repository.lastActivityAt >= :recentThreshold then 1 else 0 end), " +
                "count(distinct case when e.repository.visibility = :publicVisibility then e.repository.id else null end), " +
                "count(distinct case when e.repository.visibility = :privateVisibility then e.repository.id else null end) " +
                "from RepositoryTechnologyEvidence e " +
                "where e.user.id=:userId and e.repository.includedInAnalysis=true group by e.technology.id",
                Object[].class)
            .setParameter("userId", userId).setParameter("recentThreshold", recentThreshold)
            .setParameter("publicVisibility", RepositoryVisibility.PUBLIC)
            .setParameter("privateVisibility", RepositoryVisibility.PRIVATE).getResultList();
        return rows.stream().map(row -> new TechnologyEvidenceSummaryRow(
                (UUID) row[0], number(row[1]), number(row[2]), number(row[3]),
                (java.time.OffsetDateTime) row[4], (java.time.OffsetDateTime) row[5],
                number(row[6]), number(row[7]), number(row[8]))).toList();
    }

    public List<RepresentativeProjectRow> findRepresentativeProjects(UUID userId, UUID technologyId, int limit) {
        return entityManager.createQuery(
                "select e.repository.id, e.repository.name, e.repository.htmlUrl, " +
                "e.repository.visibility, e.repository.ownershipRelation, max(e.repository.lastActivityAt), count(e.id) " +
                "from RepositoryTechnologyEvidence e " +
                "where e.user.id=:userId and e.technology.id=:technologyId and e.repository.includedInAnalysis=true " +
                "group by e.repository.id, e.repository.name, e.repository.htmlUrl, " +
                "e.repository.visibility, e.repository.ownershipRelation " +
                "order by max(e.repository.lastActivityAt) desc nulls last, count(e.id) desc",
                Object[].class)
            .setParameter("userId", userId).setParameter("technologyId", technologyId)
            .setMaxResults(Math.max(1, Math.min(limit, 1000)))
            .getResultList().stream().map(row -> new RepresentativeProjectRow(
                    (UUID) row[0], (String) row[1], (String) row[2],
                    row[3] == null ? "UNKNOWN" : row[3].toString(),
                    row[4] == null ? "UNKNOWN" : row[4].toString(),
                    (java.time.OffsetDateTime) row[5], number(row[6]))).toList();
    }

    private int number(Object value) { return value == null ? 0 : ((Number) value).intValue(); }

    public record RepresentativeProjectRow(UUID repositoryId, String repositoryName, String htmlUrl,
                                           String visibility, String ownershipRelation,
                                           java.time.OffsetDateTime lastActivityAt, int evidenceCount) {}

    public record TechnologyEvidenceSummaryRow(UUID technologyId, int repositoryCount, int evidenceCount,
                                               int independentEvidenceTypes, java.time.OffsetDateTime firstObservedAt,
                                               java.time.OffsetDateTime lastObservedAt, int recentRepositoryCount,
                                               int publicRepositoryCount, int privateRepositoryCount) {}
}
