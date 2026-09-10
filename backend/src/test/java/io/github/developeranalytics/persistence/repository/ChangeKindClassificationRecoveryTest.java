package io.github.developeranalytics.persistence.repository;

import io.github.developeranalytics.domain.change.ChangeKind;
import io.github.developeranalytics.domain.change.ChangeKindClassification;
import io.github.developeranalytics.domain.change.ContributionFileChange;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@Tag("persistence")
class ChangeKindClassificationRecoveryTest {

    @Inject EntityManager entityManager;
    @Inject SourceRepositoryRepository repositories;
    @Inject ContributionRepository contributions;
    @Inject ContributionFileChangeRepository fileChanges;

    @Test
    void currentRepositoriesAreReconciledWhenFileClassificationIsMissingStaleOrPartial() {
        UUID[] ids = new UUID[6];

        QuarkusTransaction.requiringNew().run(() -> {
            AppUser user = AppUser.create();
            entityManager.persist(user);
            ids[0] = user.getId();

            SourceRepository missing = repository(user, "missing-classification");
            commit(user, missing, "missing", 4, 2, 1);

            SourceRepository stale = repository(user, "stale-classification");
            Contribution staleCommit = commit(user, stale, "stale", 3, 1, 1);
            entityManager.persist(new ContributionFileChange(
                    staleCommit,
                    user,
                    stale,
                    "src/Stale.java",
                    3,
                    1,
                    new ChangeKindClassification(ChangeKind.CODE, 1.0, "test-old-version", "0"),
                    staleCommit.getOccurredAt()
            ));

            SourceRepository partial = repository(user, "partial-classification");
            Contribution partialCommit = commit(user, partial, "partial", 8, 3, 2);
            entityManager.persist(new ContributionFileChange(
                    partialCommit,
                    user,
                    partial,
                    "src/OnlyOneOfTwo.java",
                    4,
                    1,
                    new ChangeKindClassifier().classify("src/OnlyOneOfTwo.java"),
                    partialCommit.getOccurredAt()
            ));

            SourceRepository zeroFiles = repository(user, "zero-files");
            commit(user, zeroFiles, "zero", 0, 0, 0);

            SourceRepository complete = repository(user, "complete-classification");
            Contribution completeCommit = commit(user, complete, "complete", 5, 2, 1);
            entityManager.persist(new ContributionFileChange(
                    completeCommit,
                    user,
                    complete,
                    "src/Complete.java",
                    5,
                    2,
                    new ChangeKindClassifier().classify("src/Complete.java"),
                    completeCommit.getOccurredAt()
            ));

            entityManager.flush();
            ids[1] = missing.getId();
            ids[2] = stale.getId();
            ids[3] = partial.getId();
            ids[4] = zeroFiles.getId();
            ids[5] = complete.getId();
        });

        QuarkusTransaction.requiringNew().run(() -> {
            Set<UUID> candidateIds = repositories.findContributionScopeUpgradeCandidates(
                            500, ChangeKindClassifier.CLASSIFIER_VERSION).stream()
                    .map(SourceRepository::getId)
                    .collect(Collectors.toSet());

            assertTrue(candidateIds.contains(ids[1]),
                    "a current repository with missing file classification must be reconciled");
            assertTrue(candidateIds.contains(ids[2]),
                    "a current repository with only stale classifier rows must be reconciled");
            assertTrue(candidateIds.contains(ids[3]),
                    "a current repository with only some expected current file rows must be reconciled");
            assertFalse(candidateIds.contains(ids[4]),
                    "a commit with complete 0/0/0 statistics needs no file classification rows");
            assertFalse(candidateIds.contains(ids[5]),
                    "a repository with current complete classification must not be requeued");

            assertTrue(contributions.findCommitsMissingFileClassification(
                            ids[0], ids[3], ChangeKindClassifier.CLASSIFIER_VERSION, 1_000).stream()
                    .anyMatch(commit -> "partial".equals(commit.getProviderContributionId())),
                    "the backfill must select a partially classified commit");
            assertTrue(fileChanges.hasMissingCurrentClassification(
                            ids[0], ids[3], ChangeKindClassifier.CLASSIFIER_VERSION),
                    "partial current classification must keep repository completion false");
        });

        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                        "delete from app_user where id=:userId")
                .setParameter("userId", ids[0])
                .executeUpdate());
    }

    private SourceRepository repository(AppUser user, String name) {
        SourceRepository repository = new SourceRepository(user, "github", name, "owner", name);
        repository.markContributionScopeCurrent();
        repository.markSynced(OffsetDateTime.now(ZoneOffset.UTC));
        entityManager.persist(repository);
        return repository;
    }

    private Contribution commit(
            AppUser user,
            SourceRepository repository,
            String externalId,
            int additions,
            int deletions,
            int changedFiles
    ) {
        Contribution commit = new Contribution(
                user,
                repository,
                "github",
                externalId,
                Contribution.Type.COMMIT,
                OffsetDateTime.now(ZoneOffset.UTC)
        );
        commit.updateFileStatistics(additions, deletions, changedFiles);
        entityManager.persist(commit);
        return commit;
    }
}
