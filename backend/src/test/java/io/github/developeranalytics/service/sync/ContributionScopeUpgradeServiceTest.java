package io.github.developeranalytics.service.sync;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class ContributionScopeUpgradeServiceTest {

    @Test
    void enqueuesOnlyCandidatesReturnedByRepositoryAndReliesOnJobDeduplication() throws Exception {
        AppUser user = AppUser.create();
        SourceRepository first = repository(user, "one");
        SourceRepository second = repository(user, "two");

        StubSourceRepositoryRepository repositories = new StubSourceRepositoryRepository(List.of(first, second));
        StubJobService jobs = new StubJobService();
        jobs.returnNullOnSecond = true;

        ContributionScopeUpgradeService service = new ContributionScopeUpgradeService();
        service.repositories = repositories;
        service.jobs = jobs;

        assertEquals(1, service.enqueueMissingBackfills(100));
        assertEquals(2, jobs.calls);
        assertEquals(100, repositories.lastLimit);
        assertEquals(ChangeKindClassifier.CLASSIFIER_VERSION, repositories.lastClassifierVersion);
    }

    @Test
    void skipsUnpersistedCandidatesWithoutRepositoryId() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(user, "github", "repo", "owner", "repo");
        StubSourceRepositoryRepository repositories = new StubSourceRepositoryRepository(List.of(repository));
        StubJobService jobs = new StubJobService();

        ContributionScopeUpgradeService service = new ContributionScopeUpgradeService();
        service.repositories = repositories;
        service.jobs = jobs;

        assertEquals(0, service.enqueueMissingBackfills(25));
        assertEquals(0, jobs.calls);
    }

    private static SourceRepository repository(AppUser user, String name) throws Exception {
        SourceRepository repository = new SourceRepository(user, "github", name, "owner", name);
        Field id = SourceRepository.class.getDeclaredField("id");
        id.setAccessible(true);
        id.set(repository, UUID.randomUUID());
        return repository;
    }

    private static final class StubSourceRepositoryRepository extends SourceRepositoryRepository {
        private final List<SourceRepository> candidates;
        private int lastLimit;
        private String lastClassifierVersion;

        private StubSourceRepositoryRepository(List<SourceRepository> candidates) {
            this.candidates = candidates;
        }

        @Override
        public List<SourceRepository> findContributionScopeUpgradeCandidates(int limit, String classifierVersion) {
            lastLimit = limit;
            lastClassifierVersion = classifierVersion;
            return candidates;
        }
    }

    private static final class StubJobService extends RepositoryDiscoveryJobService {
        private int calls;
        private boolean returnNullOnSecond;

        @Override
        public BackgroundJob enqueueChangeKindBackfill(AppUser user, UUID repositoryId) {
            calls++;
            if (returnNullOnSecond && calls == 2) return null;
            return BackgroundJob.queued(
                    user,
                    "test",
                    100,
                    java.util.Map.of("repositoryId", repositoryId.toString()),
                    1,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
        }
    }
}
