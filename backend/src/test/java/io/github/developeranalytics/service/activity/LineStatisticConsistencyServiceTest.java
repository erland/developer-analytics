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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@Tag("persistence")
class LineStatisticConsistencyServiceTest {
    @Inject EntityManager entityManager;
    @Inject LineStatisticConsistencyService service;

    @Test
    void comparesCommitTotalsWithCompleteCurrentFileClassification() {
        UUID[] userId = new UUID[1];
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        QuarkusTransaction.requiringNew().run(() -> {
            AppUser user = AppUser.create();
            entityManager.persist(user);
            userId[0] = user.getId();

            SourceRepository repository = new SourceRepository(user, "github", "line-consistency", "owner", "line-consistency");
            entityManager.persist(repository);

            Contribution matching = commit(user, repository, "matching", now, 10, 3, 2);
            file(matching, user, repository, "src/App.java", 6, 2, now);
            file(matching, user, repository, "docs/readme.md", 4, 1, now);

            Contribution mismatch = commit(user, repository, "mismatch", now.plusMinutes(1), 8, 2, 1);
            file(mismatch, user, repository, "src/Other.java", 12, 1, now.plusMinutes(1));

            Contribution incomplete = commit(user, repository, "incomplete", now.plusMinutes(2), 20, 4, 2);
            file(incomplete, user, repository, "src/Only.java", 20, 4, now.plusMinutes(2));

            Contribution zero = commit(user, repository, "zero", now.plusMinutes(3), 0, 0, 0);
            entityManager.flush();
        });

        var result = service.get(userId[0]);
        assertEquals(3, result.completeCommitCount());
        assertEquals(2, result.matchingCommitCount());
        assertEquals(1, result.mismatchingCommitCount());
        assertEquals(18, result.commitAdditions());
        assertEquals(5, result.commitDeletions());
        assertEquals(22, result.fileAdditions());
        assertEquals(4, result.fileDeletions());
        assertEquals(5, result.netLineDifference());

        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery("delete from app_user where id=:userId")
                .setParameter("userId", userId[0]).executeUpdate());
    }

    private Contribution commit(AppUser user, SourceRepository repository, String externalId,
                                OffsetDateTime at, int additions, int deletions, int changedFiles) {
        Contribution contribution = new Contribution(user, repository, "github", externalId, Contribution.Type.COMMIT, at);
        contribution.updateFileStatistics(additions, deletions, changedFiles);
        entityManager.persist(contribution);
        return contribution;
    }

    private void file(Contribution contribution, AppUser user, SourceRepository repository,
                      String path, int additions, int deletions, OffsetDateTime at) {
        entityManager.persist(new ContributionFileChange(contribution, user, repository, path, additions, deletions,
                new ChangeKindClassifier().classify(path), at));
    }
}
