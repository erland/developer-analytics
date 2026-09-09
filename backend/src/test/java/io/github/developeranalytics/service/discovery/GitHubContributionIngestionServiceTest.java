package io.github.developeranalytics.service.discovery;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderContribution;
import io.github.developeranalytics.provider.ProviderException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class GitHubContributionIngestionServiceTest {

    @Test
    void createsAndEnrichesNewCommit() throws Exception {
        FakeContributionRepository repositoryStore = new FakeContributionRepository();
        FakeCommitFileChangeService fileChanges = new FakeCommitFileChangeService(false,
                new GitHubCommitFileChangeService.CommitDetails(10, 4, 2));
        GitHubContributionIngestionService service = service(repositoryStore, fileChanges);

        AppUser user = AppUser.create();
        SourceRepository repository = sourceRepository(user);
        ProviderContribution providerContribution = contribution(
                "abc", ProviderContribution.Type.COMMIT, ProviderContribution.State.UNKNOWN,
                1, 1, 1, null);

        var result = service.ingest(user, repository, providerContribution, new ProviderAccessToken("token"));

        assertTrue(result.created());
        assertFalse(result.updated());
        assertEquals(1, repositoryStore.persistCount);
        assertEquals(1, fileChanges.refreshCount);

        Contribution stored = repositoryStore.values.get(key("abc", Contribution.Type.COMMIT));
        assertNotNull(stored);
        assertEquals(Contribution.Type.COMMIT, stored.getType());
        assertEquals("title-abc", stored.getTitle());
        assertEquals(10, stored.getAdditions());
        assertEquals(4, stored.getDeletions());
        assertEquals(2, stored.getChangedFiles());
    }

    @Test
    void preservesCachedFileStatisticsForExistingCommit() throws Exception {
        AppUser user = AppUser.create();
        SourceRepository repository = sourceRepository(user);
        Contribution existing = new Contribution(
                user, repository, "github", "cached", Contribution.Type.COMMIT, OffsetDateTime.parse("2026-09-01T10:00:00Z"));
        existing.updateFileStatistics(7, 2, 3);

        FakeContributionRepository repositoryStore = new FakeContributionRepository();
        repositoryStore.values.put(key("cached", Contribution.Type.COMMIT), existing);
        FakeCommitFileChangeService fileChanges = new FakeCommitFileChangeService(true,
                new GitHubCommitFileChangeService.CommitDetails(99, 99, 99));
        GitHubContributionIngestionService service = service(repositoryStore, fileChanges);

        var result = service.ingest(user, repository,
                contribution("cached", ProviderContribution.Type.COMMIT, ProviderContribution.State.UNKNOWN,
                        100, 100, 100, null),
                new ProviderAccessToken("token"));

        assertFalse(result.created());
        assertTrue(result.updated());
        assertEquals(0, repositoryStore.persistCount);
        assertEquals(0, fileChanges.refreshCount);
        assertEquals(7, existing.getAdditions());
        assertEquals(2, existing.getDeletions());
        assertEquals(3, existing.getChangedFiles());
    }

    @Test
    void mapsNonCommitContributionTypesWithoutCommitEnrichment() throws Exception {
        FakeContributionRepository repositoryStore = new FakeContributionRepository();
        FakeCommitFileChangeService fileChanges = new FakeCommitFileChangeService(false,
                new GitHubCommitFileChangeService.CommitDetails(1, 1, 1));
        GitHubContributionIngestionService service = service(repositoryStore, fileChanges);
        AppUser user = AppUser.create();
        SourceRepository repository = sourceRepository(user);

        service.ingest(user, repository,
                contribution("pr", ProviderContribution.Type.PULL_REQUEST, ProviderContribution.State.MERGED,
                        null, null, null, true), new ProviderAccessToken("token"));
        service.ingest(user, repository,
                contribution("issue", ProviderContribution.Type.ISSUE, ProviderContribution.State.OPEN,
                        null, null, null, null), new ProviderAccessToken("token"));
        service.ingest(user, repository,
                contribution("review", ProviderContribution.Type.REVIEW, ProviderContribution.State.CLOSED,
                        null, null, null, null), new ProviderAccessToken("token"));

        assertEquals(Contribution.Type.PULL_REQUEST,
                repositoryStore.values.get(key("pr", Contribution.Type.PULL_REQUEST)).getType());
        assertEquals(Contribution.Type.ISSUE,
                repositoryStore.values.get(key("issue", Contribution.Type.ISSUE)).getType());
        assertEquals(Contribution.Type.REVIEW,
                repositoryStore.values.get(key("review", Contribution.Type.REVIEW)).getType());
        assertEquals(0, fileChanges.refreshCount);
        assertEquals(3, repositoryStore.persistCount);
    }

    private static GitHubContributionIngestionService service(
            ContributionRepository contributions,
            GitHubCommitFileChangeService fileChanges
    ) {
        GitHubContributionIngestionService service = new GitHubContributionIngestionService();
        service.contributions = contributions;
        service.commitFileChanges = fileChanges;
        return service;
    }

    private static SourceRepository sourceRepository(AppUser user) {
        return new SourceRepository(user, "github", "repo-1", "owner", "repo");
    }

    private static ProviderContribution contribution(
            String id,
            ProviderContribution.Type type,
            ProviderContribution.State state,
            Integer additions,
            Integer deletions,
            Integer changedFiles,
            Boolean merged
    ) {
        return new ProviderContribution(
                id, type, "title-" + id, OffsetDateTime.parse("2026-09-01T10:00:00Z"),
                state, additions, deletions, changedFiles, merged);
    }

    private static String key(String id, Contribution.Type type) {
        return id + ":" + type;
    }

    static final class FakeContributionRepository extends ContributionRepository {
        final Map<String, Contribution> values = new HashMap<>();
        int persistCount;

        @Override
        public Optional<Contribution> findByProviderIdentity(
                java.util.UUID userId,
                String provider,
                String externalContributionId,
                Contribution.Type type
        ) {
            return Optional.ofNullable(values.get(key(externalContributionId, type)));
        }

        @Override
        public void persist(Contribution contribution) {
            persistCount++;
            values.put(key(contribution.getProviderContributionId(), contribution.getType()), contribution);
        }
    }

    static final class FakeCommitFileChangeService extends GitHubCommitFileChangeService {
        final boolean cached;
        final CommitDetails details;
        int refreshCount;

        FakeCommitFileChangeService(boolean cached, CommitDetails details) {
            this.cached = cached;
            this.details = details;
        }

        @Override
        public boolean hasCurrentClassification(Contribution contribution) {
            return cached;
        }

        @Override
        public CommitDetails refresh(
                AppUser user,
                SourceRepository repository,
                Contribution contribution,
                ProviderAccessToken token
        ) throws ProviderException {
            refreshCount++;
            return details;
        }
    }
}
