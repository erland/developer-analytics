package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.change.ContributionFileChange;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.service.activity.ActivityApplicationService;
import io.github.developeranalytics.service.activity.ChangeKindActivityService;
import io.github.developeranalytics.service.activity.ChangeKindCoverageService;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@Tag("persistence")
class MeActivityChangeKindFilterIntegrationTest {

    @Inject EntityManager entityManager;
    @Inject ActivityApplicationService activity;
    @Inject ChangeKindActivityService filteredActivity;
    @Inject ChangeKindCoverageService coverage;

    private final ChangeKindClassifier classifier = new ChangeKindClassifier();

    @Test
    void queryParameterRoutesToDistinctActivityAggregates() {
        AppUser[] currentUser = new AppUser[1];
        UUID[] userId = new UUID[1];

        QuarkusTransaction.requiringNew().run(() -> {
            AppUser user = AppUser.create();
            entityManager.persist(user);

            SourceRepository repository = new SourceRepository(user, "github", "api-filter-repo", "owner", "api-filter-repo");
            repository.markContributionScopeCurrent();
            repository.markSynced(OffsetDateTime.of(2026, 9, 10, 8, 0, 0, 0, ZoneOffset.UTC));
            entityManager.persist(repository);

            Contribution code = commit(user, repository, "code", 10, 2, 1, 8);
            file(code, user, repository, "src/App.java", 10, 2);

            Contribution docs = commit(user, repository, "docs", 4, 1, 1, 9);
            file(docs, user, repository, "docs/guide.md", 4, 1);

            Contribution mixed = commit(user, repository, "mixed", 11, 5, 2, 10);
            file(mixed, user, repository, "src/Mixed.java", 6, 3);
            file(mixed, user, repository, "docs/mixed.md", 5, 2);

            entityManager.flush();
            currentUser[0] = user;
            userId[0] = user.getId();
        });

        QuarkusTransaction.requiringNew().run(() -> {
            MeActivityResource resource = new MeActivityResource();
            resource.currentUserService = new CurrentUserService() {
                @Override
                public CurrentUser requireCurrentUser(String rawSessionToken) {
                    return new CurrentUser(currentUser[0], null);
                }
            };
            resource.activity = activity;
            resource.filteredActivity = filteredActivity;
            resource.changeKindCoverage = coverage;

            var all = get(resource, List.of());
            var code = get(resource, List.of("CODE"));
            var documentation = get(resource, List.of("DOCUMENTATION"));
            var combined = get(resource, List.of("CODE,DOCUMENTATION"));

            assertResponse(all, 3, 25, 8);
            assertResponse(code, 2, 16, 5);
            assertResponse(documentation, 2, 9, 3);
            assertResponse(combined, 3, 25, 8);

            assertEquals(3, code.changeKindTotalCommitCount());
            assertEquals(3, code.changeKindClassifiedCommitCount());
        });

        QuarkusTransaction.requiringNew().run(() -> entityManager.createNativeQuery(
                        "delete from app_user where id=:userId")
                .setParameter("userId", userId[0])
                .executeUpdate());
    }

    private static MeActivityResource.ActivityResponse get(MeActivityResource resource, List<String> changeKinds) {
        return resource.get(
                "test-session",
                null, null, null, null, null,
                null, null, null,
                List.of(), List.of(), changeKinds
        );
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
        Contribution contribution = new Contribution(
                user, repository, "github", externalId, Contribution.Type.COMMIT,
                OffsetDateTime.of(2026, 9, 8, hour, 0, 0, 0, ZoneOffset.UTC)
        );
        contribution.updateFileStatistics(additions, deletions, changedFiles);
        entityManager.persist(contribution);
        return contribution;
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
                contribution, user, repository, path, additions, deletions,
                classifier.classify(path), contribution.getOccurredAt()
        ));
    }

    private static void assertResponse(MeActivityResource.ActivityResponse response, int commits, long additions, long deletions) {
        assertEquals(commits, response.commitCount());
        assertEquals(additions, response.additions());
        assertEquals(deletions, response.deletions());
    }
}
