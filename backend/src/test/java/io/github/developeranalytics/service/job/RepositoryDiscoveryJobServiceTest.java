package io.github.developeranalytics.service.job;

import io.github.developeranalytics.domain.job.BackgroundJobStatus;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.worker.GitHubRepositoryDiscoveryJobHandler;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Tag("worker-job")
@Tag("persistence")
class RepositoryDiscoveryJobServiceTest {

    @Inject
    EntityManager entityManager;

    @Inject
    RepositoryDiscoveryJobService jobs;

    @Test
    @Transactional
    void queuesGitHubRepositoryDiscoveryForCurrentUser() {
        AppUser user = AppUser.create();
        entityManager.persist(user);
        entityManager.flush();

        var job = jobs.enqueueGitHubDiscovery(user);
        entityManager.flush();

        assertNotNull(job.getId());
        assertEquals(GitHubRepositoryDiscoveryJobHandler.JOB_TYPE, job.getJobType());
        assertEquals(BackgroundJobStatus.QUEUED, job.getStatus());
        assertEquals(user.getId(), job.getUser().getId());
    }
}
