package io.github.developeranalytics.service.activity;

import io.github.developeranalytics.domain.change.ContributionFileChange;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.RepositoryUserActivityWeekRepository;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
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

@QuarkusTest
@Tag("persistence")
class ChangeKindCoverageServiceTest {

    @Inject EntityManager entityManager;
    @Inject ChangeKindCoverageService coverage;
    @Inject RepositoryUserActivityWeekRepository weeklyActivity;

    @Test
    void coverageUsesAllCommitsInScopeAndRequiresCompleteCurrentClassification() {
        UUID[] userId = new UUID[1];
        LocalDate day = LocalDate.of(2026, 9, 10);
        LocalDate weekStart = day.minusDays(day.getDayOfWeek().getValue() - 1L);
        OffsetDateTime occurredAt = day.atTime(12, 0).atOffset(ZoneOffset.UTC);

        QuarkusTransaction.requiringNew().run(() -> {
            AppUser user = AppUser.create();
            entityManager.persist(user);
            userId[0] = user.getId();

            SourceRepository repository = new SourceRepository(user, "github", "coverage-repo", "owner", "coverage-repo");
            repository.markContributionScopeCurrent();
            repository.markSynced(occurredAt);
            repository.setLastActivityAt(occurredAt);
            entityManager.persist(repository);

            Contribution complete = commit(user, repository, "complete", occurredAt, 7, 2, 2);
            entityManager.persist(new ContributionFileChange(
                    complete, user, repository, "src/App.java", 5, 1,
                    new ChangeKindClassifier().classify("src/App.java"), occurredAt));
            entityManager.persist(new ContributionFileChange(
                    complete, user, repository, "docs/readme.md", 2, 1,
                    new ChangeKindClassifier().classify("docs/readme.md"), occurredAt));

            commit(user, repository, "missing", occurredAt.plusMinutes(1), 3, 1, 1);
            commit(user, repository, "zero", occurredAt.plusMinutes(2), 0, 0, 0);

            Contribution partial = commit(user, repository, "partial", occurredAt.plusMinutes(3), 4, 1, 2);
            entityManager.persist(new ContributionFileChange(
                    partial, user, repository, "src/OnlyOne.java", 4, 1,
                    new ChangeKindClassifier().classify("src/OnlyOne.java"), partial.getOccurredAt()));

            entityManager.flush();
            weeklyActivity.replace(
                    user.getId(), repository.getId(),
                    List.of(new RepositoryUserActivityWeekRepository.WeekInput(weekStart, 4, 14, 4)),
                    occurredAt.plusHours(1)
            );
        });

        ChangeKindCoverageService.Coverage result = coverage.get(
                userId[0], day, day, null, null, null, List.of(), List.of());

        assertEquals(4, result.totalCommitCount());
        assertEquals(2, result.classifiedCommitCount(),
                "only the fully classified commit and the valid zero-file commit should count as covered");

        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                        "delete from app_user where id=:userId")
                .setParameter("userId", userId[0])
                .executeUpdate());
    }

    private Contribution commit(
            AppUser user,
            SourceRepository repository,
            String externalId,
            OffsetDateTime occurredAt,
            int additions,
            int deletions,
            int changedFiles
    ) {
        Contribution contribution = new Contribution(
                user, repository, "github", externalId, Contribution.Type.COMMIT, occurredAt);
        contribution.updateFileStatistics(additions, deletions, changedFiles);
        entityManager.persist(contribution);
        return contribution;
    }
}
