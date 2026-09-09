package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.change.ChangeKind;
import io.github.developeranalytics.domain.change.ContributionFileChange;
import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.ContributionFileChangeRepository;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.provider.ProviderContributionFileChange;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.history.ContributionHistoryProvider;
import io.github.developeranalytics.provider.history.HistoricalCommitFileChanges;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
import io.github.developeranalytics.service.connection.ProviderCredentialService;
import io.github.developeranalytics.service.discovery.GitHubCommitFileChangeService;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("worker-job")
class GitHubChangeKindBackfillJobHandlerTest {

    @Test
    void usesGitHistoryAsPrimarySourceAndMarksScopeCurrent() throws Exception {
        Fixture fixture = new Fixture(false);
        Contribution commit = fixture.commit("git-sha");
        fixture.contributions.batch = List.of(commit);
        fixture.history = (token, repository, shas) -> List.of(
                new HistoricalCommitFileChanges("git-sha", List.of(
                        new ProviderContributionFileChange("src/Main.java", 5, 2),
                        new ProviderContributionFileChange("README.md", 3, 1)
                ))
        );

        fixture.handler().handle(fixture.job());

        assertEquals(0, fixture.rest.refreshCount);
        assertEquals(2, fixture.fileChanges.persisted.size());
        assertEquals(ChangeKind.CODE, fixture.fileChanges.persisted.get(0).getChangeKind());
        assertEquals(ChangeKind.DOCUMENTATION, fixture.fileChanges.persisted.get(1).getChangeKind());
        assertEquals(8, commit.getAdditions());
        assertEquals(3, commit.getDeletions());
        assertEquals(2, commit.getChangedFiles());
        assertEquals(SourceRepository.CURRENT_CONTRIBUTION_SCOPE_VERSION,
                fixture.repository.getContributionScopeVersion());
        assertEquals(0, fixture.jobs.continuationCount);
    }

    @Test
    void fallsBackToRestOnlyForCommitMissingFromGitResultAndQueuesContinuation() throws Exception {
        Fixture fixture = new Fixture(true);
        Contribution gitCommit = fixture.commit("git-sha");
        Contribution restCommit = fixture.commit("rest-sha");
        fixture.contributions.batch = List.of(gitCommit, restCommit);
        fixture.history = (token, repository, shas) -> List.of(
                new HistoricalCommitFileChanges("git-sha", List.of(
                        new ProviderContributionFileChange("src/Main.java", 4, 1)
                ))
        );
        fixture.rest.details = new GitHubCommitFileChangeService.CommitDetails(7, 3, 2);

        fixture.handler().handle(fixture.job());

        assertEquals(1, fixture.rest.refreshCount);
        assertEquals("rest-sha", fixture.rest.lastCommitSha);
        assertEquals(4, gitCommit.getAdditions());
        assertEquals(1, gitCommit.getDeletions());
        assertEquals(1, gitCommit.getChangedFiles());
        assertEquals(7, restCommit.getAdditions());
        assertEquals(3, restCommit.getDeletions());
        assertEquals(2, restCommit.getChangedFiles());
        assertEquals(1, fixture.jobs.continuationCount);
        assertEquals(0, fixture.repository.getContributionScopeVersion());
    }

    @Test
    void fallsBackToRestForWholeBatchWhenGitHistoryFails() throws Exception {
        Fixture fixture = new Fixture(false);
        Contribution first = fixture.commit("first");
        Contribution second = fixture.commit("second");
        fixture.contributions.batch = List.of(first, second);
        fixture.history = (token, repository, shas) -> {
            throw new ProviderException("git unavailable", 0);
        };
        fixture.rest.details = new GitHubCommitFileChangeService.CommitDetails(2, 1, 1);

        fixture.handler().handle(fixture.job());

        assertEquals(2, fixture.rest.refreshCount);
        assertEquals(2, first.getAdditions());
        assertEquals(1, first.getDeletions());
        assertEquals(1, first.getChangedFiles());
        assertEquals(2, second.getAdditions());
        assertEquals(1, second.getDeletions());
        assertEquals(1, second.getChangedFiles());
        assertEquals(SourceRepository.CURRENT_CONTRIBUTION_SCOPE_VERSION,
                fixture.repository.getContributionScopeVersion());
    }

    private static final class Fixture {
        private final UUID repositoryId = UUID.randomUUID();
        private final AppUser user = AppUser.create();
        private final SourceRepository repository =
                new SourceRepository(user, "github", "repository-1", "owner", "repo");
        private final StubSourceRepositoryRepository repositories =
                new StubSourceRepositoryRepository(repository);
        private final StubContributionRepository contributions = new StubContributionRepository();
        private final StubContributionFileChangeRepository fileChanges;
        private final StubCredentialService credentials = new StubCredentialService();
        private final StubCommitFileChangeService rest = new StubCommitFileChangeService();
        private final StubJobService jobs = new StubJobService();
        private ContributionHistoryProvider history;

        private Fixture(boolean remaining) {
            fileChanges = new StubContributionFileChangeRepository(remaining);
        }

        private Contribution commit(String sha) {
            return new Contribution(
                    user,
                    repository,
                    "github",
                    sha,
                    Contribution.Type.COMMIT,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
        }

        private BackgroundJob job() {
            return BackgroundJob.queued(
                    user,
                    GitHubChangeKindBackfillJobHandler.JOB_TYPE,
                    115,
                    Map.of("provider", "github", "repositoryId", repositoryId.toString()),
                    5,
                    OffsetDateTime.now(ZoneOffset.UTC)
            );
        }

        private GitHubChangeKindBackfillJobHandler handler() {
            GitHubChangeKindBackfillJobHandler handler = new GitHubChangeKindBackfillJobHandler();
            handler.repositories = repositories;
            handler.contributions = contributions;
            handler.fileChanges = fileChanges;
            handler.credentials = credentials;
            handler.contributionHistory = history;
            handler.commitFileChanges = rest;
            handler.classifier = new ChangeKindClassifier();
            handler.jobs = jobs;
            return handler;
        }
    }

    private static final class StubSourceRepositoryRepository extends SourceRepositoryRepository {
        private final SourceRepository repository;

        private StubSourceRepositoryRepository(SourceRepository repository) {
            this.repository = repository;
        }

        @Override
        public Optional<SourceRepository> findByIdForUser(UUID repositoryId, UUID userId) {
            return Optional.of(repository);
        }
    }

    private static final class StubContributionRepository extends ContributionRepository {
        private List<Contribution> batch = List.of();

        @Override
        public List<Contribution> findCommitsMissingFileClassification(
                UUID userId, UUID repositoryId, String classifierVersion, int limit) {
            return batch;
        }
    }

    private static final class StubContributionFileChangeRepository extends ContributionFileChangeRepository {
        private final boolean remaining;
        private final List<ContributionFileChange> persisted = new ArrayList<>();

        private StubContributionFileChangeRepository(boolean remaining) {
            this.remaining = remaining;
        }

        @Override
        public void persist(ContributionFileChange change) {
            persisted.add(change);
        }

        @Override
        public int deleteForContribution(Contribution contribution) {
            return 0;
        }

        @Override
        public boolean hasMissingCurrentClassification(UUID userId, UUID repositoryId, String classifierVersion) {
            return remaining;
        }
    }

    private static final class StubCredentialService extends ProviderCredentialService {
        @Override
        public ProviderAccessToken requireAccessToken(UUID userId, String provider) {
            return new ProviderAccessToken("test-token");
        }
    }

    private static final class StubCommitFileChangeService extends GitHubCommitFileChangeService {
        private CommitDetails details = new CommitDetails(1, 1, 1);
        private int refreshCount;
        private String lastCommitSha;

        @Override
        public CommitDetails refresh(
                AppUser user,
                SourceRepository repository,
                Contribution contribution,
                ProviderAccessToken token
        ) {
            refreshCount++;
            lastCommitSha = contribution.getProviderContributionId();
            return details;
        }
    }

    private static final class StubJobService extends RepositoryDiscoveryJobService {
        private int continuationCount;

        @Override
        public BackgroundJob enqueueChangeKindBackfillContinuation(
                AppUser user, UUID repositoryId, UUID currentJobId) {
            continuationCount++;
            return null;
        }
    }
}
