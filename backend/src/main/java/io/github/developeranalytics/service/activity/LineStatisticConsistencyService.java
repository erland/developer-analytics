package io.github.developeranalytics.service.activity;

import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class LineStatisticConsistencyService {
    @Inject EntityManager entityManager;

    @Transactional
    public Result get(UUID userId) {
        List<Object[]> rows = entityManager.createQuery(
                "select c.id, c.repository.id, c.repository.name, c.additions, c.deletions, c.changedFiles, " +
                        "count(f.id), coalesce(sum(f.additions),0), coalesce(sum(f.deletions),0) " +
                        "from Contribution c left join ContributionFileChange f on f.contribution=c and f.classifierVersion=:version " +
                        "where c.user.id=:userId and c.type=:type and c.additions is not null and c.deletions is not null and c.changedFiles is not null " +
                        "group by c.id, c.repository.id, c.repository.name, c.additions, c.deletions, c.changedFiles",
                Object[].class)
                .setParameter("version", ChangeKindClassifier.CLASSIFIER_VERSION)
                .setParameter("userId", userId)
                .setParameter("type", Contribution.Type.COMMIT)
                .getResultList();

        long complete = 0;
        long matching = 0;
        long mismatching = 0;
        long commitAdditions = 0;
        long commitDeletions = 0;
        long fileAdditions = 0;
        long fileDeletions = 0;

        for (Object[] row : rows) {
            int changedFiles = ((Number) row[5]).intValue();
            long fileRows = ((Number) row[6]).longValue();
            boolean classificationComplete = changedFiles == 0 ? fileRows == 0 : fileRows == changedFiles;
            if (!classificationComplete) continue;

            long cAdd = ((Number) row[3]).longValue();
            long cDel = ((Number) row[4]).longValue();
            long fAdd = ((Number) row[7]).longValue();
            long fDel = ((Number) row[8]).longValue();

            complete++;
            commitAdditions += cAdd;
            commitDeletions += cDel;
            fileAdditions += fAdd;
            fileDeletions += fDel;
            if (cAdd == fAdd && cDel == fDel) matching++; else mismatching++;
        }

        return new Result(complete, matching, mismatching,
                commitAdditions, commitDeletions, fileAdditions, fileDeletions,
                (fileAdditions - fileDeletions) - (commitAdditions - commitDeletions));
    }

    public record Result(long completeCommitCount, long matchingCommitCount, long mismatchingCommitCount,
                         long commitAdditions, long commitDeletions,
                         long fileAdditions, long fileDeletions,
                         long netLineDifference) {}
}
