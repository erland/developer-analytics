package io.github.developeranalytics.service.activity;

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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static io.github.developeranalytics.domain.change.ChangeKind.CODE;
import static io.github.developeranalytics.domain.change.ChangeKind.DOCUMENTATION;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@Tag("persistence")
class ChangeKindActivityFilterIntegrationTest {

    @Inject EntityManager entityManager;
    @Inject ActivityApplicationService activity;
    @Inject ChangeKindActivityService filteredActivity;
    @Inject ChangeKindCoverageService coverage;

    private final ChangeKindClassifier classifier = new ChangeKindClassifier();

    @Test
    void allCodeDocumentationAndCombinedSelectionsProduceDifferentAggregates() {
        UUID[] ids = new UUID[2];

        QuarkusTransaction.requiringNew().run(() -> {
            AppUser user = AppUser.create();
            entityManager.persist(user);

            SourceRepository repository = new SourceRepository(user, "github", "filter-repo", "owner", "filter-repo");
            repository.markContributionScopeCurrent();
            repository.markSynced(OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, ZoneOffset.UTC));
            entityManager.persist(repository);

            Contribution codeOnly = commit(user, repository, "code-only", 10, 2, 1, 8);
            file(codeOnly, user, repository, "src/App.java", 10, 2);

            Contribution docsOnly = commit(user, repository, "docs-only", 4, 1, 1, 9);
            file(docsOnly, user, repository, "docs/guide.md", 4, 1);

            Contribution mixed = commit(user, repository, "mixed", 11, 5, 2, 10);
            file(mixed, user, repository, "src/Mixed.java", 6, 3);
            file(mixed, user, repository, "docs/mixed.md", 5, 2);

            // Complete commit-level statistics but no file classification yet. This must remain in
            // All while being absent from filtered results and lowering classification coverage.
            commit(user, repository, "pending-classification", 7, 1, 1, 11);

            entityManager.flush();
            ids[0] = user.getId();
            ids[1] = repository.getId();
        });

        QuarkusTransaction.requiringNew().run(() -> {
            var all = activity.get(ids[0], null, null, null, null, null, List.of(), List.of());
            var code = filteredActivity.get(ids[0], null, null, null, null, null, List.of(), List.of(), Set.of(CODE));
            var documentation = filteredActivity.get(ids[0], null, null, null, null, null, List.of(), List.of(), Set.of(DOCUMENTATION));
            var combined = filteredActivity.get(ids[0], null, null, null, null, null, List.of(), List.of(), Set.of(CODE, DOCUMENTATION));
            var classificationCoverage = coverage.get(ids[0], null, null, null, null, null, List.of(), List.of());

            assertActivity(all, 4, 32, 9);
            assertActivity(code, 2, 16, 5);
            assertActivity(documentation, 2, 9, 3);
            // The mixed commit belongs to both selected kinds but must count only once.
            assertActivity(combined, 3, 25, 8);

            assertEquals(4, classificationCoverage.totalCommitCount());
            assertEquals(3, classificationCoverage.classifiedCommitCount());
        });

        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                        "delete from app_user where id=:userId")
                .setParameter("userId", ids[0])
                .executeUpdate());
    }

    private Contribution commit(
            AppUser user,
            SourceRepository repository,
            String externalId,
            int additions,
            int deletions,
            int changedFiles,
            int hour
    ) {
        Contribution commit = new Contribution(
                user,
                repository,
                "github",
                externalId,
                Contribution.Type.COMMIT,
                OffsetDateTime.of(2026, 9, 8, hour, 0, 0, 0, ZoneOffset.UTC)
        );
        commit.updateFileStatistics(additions, deletions, changedFiles);
        entityManager.persist(commit);
        return commit;
    }

    private void file(
            Contribution contribution,
            AppUser user,
            SourceRepository repository,
            String path,
            int additions,
            int deletions
    ) {
        entityManager.persist(new ContributionFileChange(
                contribution,
                user,
                repository,
                path,
                additions,
                deletions,
                classifier.classify(path),
                contribution.getOccurredAt()
        ));
    }

    private static void assertActivity(
            ActivityApplicationService.ActivityResult result,
            int commits,
            long additions,
            long deletions
    ) {
        assertEquals(commits, result.commitCount());
        assertEquals(additions, result.additions());
        assertEquals(deletions, result.deletions());
        assertEquals(additions + deletions, result.additions() + result.deletions());
    }
}
