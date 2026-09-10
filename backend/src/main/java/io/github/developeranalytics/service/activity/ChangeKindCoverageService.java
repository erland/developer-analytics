package io.github.developeranalytics.service.activity;

import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.persistence.project.ProjectInventoryRepository;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Reports how much of the commit population in the current analysis scope has a complete,
 * current change-kind classification. Coverage is intentionally independent of the selected
 * change kinds: otherwise unclassified commits would disappear from the denominator and make a
 * filtered view look more complete than it really is.
 */
@ApplicationScoped
public class ChangeKindCoverageService {

    @Inject EntityManager entityManager;
    @Inject ProjectInventoryRepository projectInventory;

    @Transactional
    public Coverage get(
            UUID userId,
            LocalDate from,
            LocalDate to,
            String search,
            String ownership,
            String visibility,
            List<String> selectedProjectTypes,
            List<String> technologiesFilter
    ) {
        var matchingProjects = projectInventory.find(userId, 0, 1, search, ownership, visibility, null,
                selectedProjectTypes, technologiesFilter, from, to);
        Set<UUID> repositoryIds = new HashSet<>(matchingProjects.matchingRepositoryIds());
        if (repositoryIds.isEmpty()) return new Coverage(0, 0);

        OffsetDateTime fromDate = from == null ? null : from.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime toDate = to == null ? null : to.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        String baseWhere = " from Contribution c where c.user.id=:userId and c.type=:type "
                + "and c.repository.id in :repositoryIds";
        StringBuilder totalJpql = new StringBuilder("select count(c.id)").append(baseWhere);
        StringBuilder classifiedJpql = new StringBuilder("select count(c.id)").append(baseWhere)
                .append(" and c.additions is not null and c.deletions is not null and c.changedFiles is not null")
                .append(" and (c.changedFiles=0 or (select count(f.id) from ContributionFileChange f ")
                .append("where f.contribution=c and f.classifierVersion=:classifierVersion)=c.changedFiles)");

        if (fromDate != null) {
            totalJpql.append(" and c.occurredAt>=:fromDate");
            classifiedJpql.append(" and c.occurredAt>=:fromDate");
        }
        if (toDate != null) {
            totalJpql.append(" and c.occurredAt<:toDate");
            classifiedJpql.append(" and c.occurredAt<:toDate");
        }

        var totalQuery = entityManager.createQuery(totalJpql.toString(), Long.class)
                .setParameter("userId", userId)
                .setParameter("type", Contribution.Type.COMMIT)
                .setParameter("repositoryIds", repositoryIds);
        var classifiedQuery = entityManager.createQuery(classifiedJpql.toString(), Long.class)
                .setParameter("userId", userId)
                .setParameter("type", Contribution.Type.COMMIT)
                .setParameter("repositoryIds", repositoryIds)
                .setParameter("classifierVersion", ChangeKindClassifier.CLASSIFIER_VERSION);
        if (fromDate != null) {
            totalQuery.setParameter("fromDate", fromDate);
            classifiedQuery.setParameter("fromDate", fromDate);
        }
        if (toDate != null) {
            totalQuery.setParameter("toDate", toDate);
            classifiedQuery.setParameter("toDate", toDate);
        }

        return new Coverage(totalQuery.getSingleResult().intValue(), classifiedQuery.getSingleResult().intValue());
    }

    public record Coverage(int totalCommitCount, int classifiedCommitCount) {}
}
