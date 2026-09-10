package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.provider.ProviderContributorActivityWeek;
import io.github.developeranalytics.provider.ProviderContributorSnapshot;
import io.github.developeranalytics.provider.ProviderContributorStatistics;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class ContributorSnapshotPersistenceServiceTest {

    @Test
    void persistsContributorTotalsWithoutDependingOnWeeklyActivityStorage() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(user, "github", "repo-1", "alice", "demo");
        UUID userId = UUID.randomUUID();
        OffsetDateTime observedAt = OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        List<ProviderContributorActivityWeek> weeks = List.of(
                new ProviderContributorActivityWeek(LocalDate.of(2026, 8, 24), 3, 120, 20),
                new ProviderContributorActivityWeek(LocalDate.of(2026, 8, 31), 5, 200, 40));

        ProviderContributorSnapshot snapshot = new ProviderContributorSnapshot(
                new ProviderContributorStatistics(8, 7, 1, 11, 42, 320, 60, observedAt),
                weeks);

        ContributorSnapshotPersistenceService service = new ContributorSnapshotPersistenceService();
        service.repositories = new FakeSourceRepositoryRepository(repository);

        service.persist(userId, repository, snapshot);

        assertEquals(8, repository.getContributorCount());
        assertEquals(7, repository.getHumanContributorCount());
        assertEquals(1, repository.getBotContributorCount());
        assertEquals(11, repository.getUserCommitCount());
        assertEquals(42, repository.getRepositoryCommitCount());
        assertEquals(320L, repository.getUserAdditions());
        assertEquals(60L, repository.getUserDeletions());
        assertEquals(observedAt, repository.getContributorStatsAt());
    }

    private static final class FakeSourceRepositoryRepository extends SourceRepositoryRepository {
        private final SourceRepository repository;

        private FakeSourceRepositoryRepository(SourceRepository repository) {
            this.repository = repository;
        }

        @Override
        public Optional<SourceRepository> findByIdForUser(UUID repositoryId, UUID userId) {
            return Optional.of(repository);
        }
    }
}
