package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.job.BackgroundJobStatus;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.service.discovery.GitHubContributionDiscoveryService;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Tag("worker-job")
class GitHubContributionDiscoveryJobHandlerTest {

    @Test
    void resumesFromSavedCursorAndPersistsNextCursorWhenRateLimitPauses() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID repositoryId = UUID.randomUUID();
        AppUser user = mock(AppUser.class);
        SourceRepository repository = mock(SourceRepository.class);
        when(user.getId()).thenReturn(userId);
        when(repository.getId()).thenReturn(repositoryId);
        when(repository.getContributionScopeVersion()).thenReturn(1);

        SourceRepositoryRepository repositories = mock(SourceRepositoryRepository.class);
        ContributionRepository contributions = mock(ContributionRepository.class);
        GitHubContributionDiscoveryService discovery = mock(GitHubContributionDiscoveryService.class);
        RepositoryDiscoveryJobService jobs = mock(RepositoryDiscoveryJobService.class);
        when(repositories.findByIdForUser(repositoryId, userId)).thenReturn(Optional.of(repository));

        OffsetDateTime now = OffsetDateTime.of(2026, 9, 9, 15, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime resumeAt = now.plusMinutes(30);
        BackgroundJob job = BackgroundJob.queued(
                user,
                GitHubContributionDiscoveryJobHandler.JOB_TYPE,
                100,
                Map.of("repositoryId", repositoryId.toString(),
                        GitHubContributionDiscoveryJobHandler.CONTINUATION_CURSOR, "4"),
                5,
                now
        );
        job.markRunning("worker-1", now);

        when(discovery.discover(eq(user), eq(repository), isNull(), eq("4"), any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Consumer<String> checkpoint = invocation.getArgument(4, Consumer.class);
                    checkpoint.accept("5");
                    return new GitHubContributionDiscoveryService.DiscoveryResult(
                            UUID.randomUUID(), repositoryId, 100, 100, 0, 1,
                            false, "5", resumeAt);
                });

        GitHubContributionDiscoveryJobHandler handler = new GitHubContributionDiscoveryJobHandler();
        handler.repositories = repositories;
        handler.contributions = contributions;
        handler.discovery = discovery;
        handler.jobs = jobs;

        handler.handle(job);

        assertEquals("5", job.getPayload().get(GitHubContributionDiscoveryJobHandler.CONTINUATION_CURSOR));
        assertEquals(BackgroundJobStatus.PAUSED_RATE_LIMIT, job.getStatus());
        assertEquals(0, job.getAttemptCount());
        assertEquals(resumeAt, job.getNextExecutionAt());
        assertNull(job.getLastError());
        verifyNoInteractions(jobs);
    }

    @Test
    void clearsSavedCursorOnlyAfterDiscoveryCompletes() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID repositoryId = UUID.randomUUID();
        AppUser user = mock(AppUser.class);
        SourceRepository repository = mock(SourceRepository.class);
        when(user.getId()).thenReturn(userId);
        when(repository.getId()).thenReturn(repositoryId);
        when(repository.getContributionScopeVersion()).thenReturn(1);

        SourceRepositoryRepository repositories = mock(SourceRepositoryRepository.class);
        ContributionRepository contributions = mock(ContributionRepository.class);
        GitHubContributionDiscoveryService discovery = mock(GitHubContributionDiscoveryService.class);
        RepositoryDiscoveryJobService jobs = mock(RepositoryDiscoveryJobService.class);
        when(repositories.findByIdForUser(repositoryId, userId)).thenReturn(Optional.of(repository));
        when(discovery.discover(eq(user), eq(repository), isNull(), eq("5"), any()))
                .thenReturn(new GitHubContributionDiscoveryService.DiscoveryResult(
                        UUID.randomUUID(), repositoryId, 40, 40, 0, 1,
                        true, null, null));

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        BackgroundJob job = BackgroundJob.queued(
                user,
                GitHubContributionDiscoveryJobHandler.JOB_TYPE,
                100,
                Map.of("repositoryId", repositoryId.toString(),
                        GitHubContributionDiscoveryJobHandler.CONTINUATION_CURSOR, "5"),
                5,
                now
        );
        job.markRunning("worker-1", now);

        GitHubContributionDiscoveryJobHandler handler = new GitHubContributionDiscoveryJobHandler();
        handler.repositories = repositories;
        handler.contributions = contributions;
        handler.discovery = discovery;
        handler.jobs = jobs;

        handler.handle(job);

        assertFalse(job.getPayload().containsKey(GitHubContributionDiscoveryJobHandler.CONTINUATION_CURSOR));
        assertEquals(BackgroundJobStatus.RUNNING, job.getStatus());
        verify(jobs).enqueueChangeKindBackfill(user, repositoryId);
    }
}
