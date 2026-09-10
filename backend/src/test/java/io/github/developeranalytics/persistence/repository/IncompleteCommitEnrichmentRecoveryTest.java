package io.github.developeranalytics.persistence.repository;

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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@Tag("persistence")
class IncompleteCommitEnrichmentRecoveryTest {

    @Inject EntityManager entityManager;
    @Inject SourceRepositoryRepository repositories;
    @Inject ContributionRepository contributions;
    @Inject ContributionFileChangeRepository fileChanges;

    @Test
    void currentRepositoryWithMissingLineStatisticsIsRediscoveredAndBackfilledIncrementally() {
        UUID[] ids = new UUID[2];

        QuarkusTransaction.requiringNew().run(() -> {
            AppUser user = AppUser.create();
            entityManager.persist(user);

            SourceRepository repository = new SourceRepository(
                    user, "github", "recovery-repo", "owner", "recovery-repo");
            repository.markContributionScopeCurrent();
            repository.markSynced(OffsetDateTime.now(ZoneOffset.UTC));
            entityManager.persist(repository);

            Contribution commit = new Contribution(
                    user,
                    repository,
                    "github",
                    "commit-with-partial-stats",
                    Contribution.Type.COMMIT,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
            // This reproduces the old false-complete case: changedFiles was known to be zero,
            // while additions/deletions were still missing after a transient provider failure.
            commit.updateFromDiscovery(null, commit.getOccurredAt(), Contribution.State.UNKNOWN,
                    null, null, 0, null);
            entityManager.persist(commit);
            entityManager.flush();

            ids[0] = user.getId();
            ids[1] = repository.getId();
        });

        QuarkusTransaction.requiringNew().run(() -> {
            assertTrue(
                    repositories.findContributionScopeUpgradeCandidates(500).stream()
                            .anyMatch(repository -> ids[1].equals(repository.getId())),
                    "a repository already marked current must still be reconciled when line statistics are incomplete"
            );

            var missing = contributions.findCommitsMissingFileClassification(
                    ids[0], ids[1], ChangeKindClassifier.CLASSIFIER_VERSION, 1_000);
            assertTrue(
                    missing.stream().anyMatch(commit -> "commit-with-partial-stats".equals(commit.getProviderContributionId())),
                    "the backfill must include commits with missing additions/deletions even when changedFiles is zero"
            );
            assertTrue(
                    fileChanges.hasMissingCurrentClassification(
                            ids[0], ids[1], ChangeKindClassifier.CLASSIFIER_VERSION),
                    "repository completion must remain false while commit line statistics are incomplete"
            );

            missing.getFirst().updateFileStatistics(0, 0, 0);
        });

        QuarkusTransaction.requiringNew().run(() -> {
            assertFalse(
                    contributions.findCommitsMissingFileClassification(
                            ids[0], ids[1], ChangeKindClassifier.CLASSIFIER_VERSION, 1_000)
                            .stream()
                            .anyMatch(commit -> "commit-with-partial-stats".equals(commit.getProviderContributionId())),
                    "the repaired commit must not be selected again"
            );
            assertFalse(
                    fileChanges.hasMissingCurrentClassification(
                            ids[0], ids[1], ChangeKindClassifier.CLASSIFIER_VERSION),
                    "repository completion should become true after line statistics have been repaired"
            );
            assertFalse(
                    repositories.findContributionScopeUpgradeCandidates(500).stream()
                            .anyMatch(repository -> ids[1].equals(repository.getId())),
                    "a fully repaired current repository must leave the reconciliation queue"
            );
        });

        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                        "delete from app_user where id=:userId")
                .setParameter("userId", ids[0])
                .executeUpdate());
    }
}
