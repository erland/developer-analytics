package io.github.developeranalytics.service.activity;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@Tag("persistence")
class ActivityContributionAggregationTest {

    @Inject EntityManager entityManager;
    @Inject ActivityApplicationService activity;

    @Test
    void allActivityUsesContributionLineStatisticsWithoutWeeklyAggregateRows() {
        UUID[] userId = new UUID[1];
        LocalDate day = LocalDate.of(2026, 9, 10);
        OffsetDateTime at = day.atTime(12, 0).atOffset(ZoneOffset.UTC);

        QuarkusTransaction.requiringNew().run(() -> {
            AppUser user = AppUser.create();
            entityManager.persist(user);
            userId[0] = user.getId();

            SourceRepository repository = new SourceRepository(user, "github", "raw-activity", "owner", "raw-activity");
            repository.markSynced(at);
            repository.setLastActivityAt(at.plusMinutes(2));
            entityManager.persist(repository);

            commit(user, repository, "measured-1", at, 10, 3, 2);
            commit(user, repository, "measured-2", at.plusMinutes(1), 5, 1, 1);
            Contribution unmeasured = new Contribution(
                    user, repository, "github", "unmeasured", Contribution.Type.COMMIT, at.plusMinutes(2));
            entityManager.persist(unmeasured);
            entityManager.flush();
        });

        var result = activity.get(userId[0], day, day, null, null, null, List.of(), List.of());

        assertEquals(3, result.commitCount());
        assertEquals(2, result.lineStatisticsCommitCount());
        assertEquals(15, result.additions());
        assertEquals(4, result.deletions());
        assertEquals(9.5, result.averageCommitSize(), 0.001);
        assertTrue(result.commitSizeStatisticsAvailable());
        assertEquals(1, result.activeProjects());
        assertEquals(1, result.commitsPerMonth().size());
        assertEquals(3, result.commitsPerMonth().get(0).commits());
        assertEquals(2, result.commitsPerMonth().get(0).lineStatisticsCommitCount());
        assertEquals(15, result.commitsPerMonth().get(0).additions());
        assertEquals(4, result.commitsPerMonth().get(0).deletions());

        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                        "delete from app_user where id=:userId")
                .setParameter("userId", userId[0])
                .executeUpdate());
    }

    private void commit(AppUser user, SourceRepository repository, String externalId,
                        OffsetDateTime occurredAt, int additions, int deletions, int changedFiles) {
        Contribution contribution = new Contribution(
                user, repository, "github", externalId, Contribution.Type.COMMIT, occurredAt);
        contribution.updateFileStatistics(additions, deletions, changedFiles);
        entityManager.persist(contribution);
    }
}
