package io.github.developeranalytics.service.sync;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
class ContributionSyncOrchestratorTest {

    @Inject
    EntityManager entityManager;

    @Inject
    ContributionSyncOrchestrator orchestrator;

    @Test
    @Transactional
    void batchesRepositoriesAndAvoidsDuplicateActiveJobs() {
        AppUser user = AppUser.create();
        entityManager.persist(user);

        SourceRepository one = new SourceRepository(
                user, "github", "repo-1", "alice", "one");
        SourceRepository two = new SourceRepository(
                user, "github", "repo-2", "alice", "two");

        entityManager.persist(one);
        entityManager.persist(two);
        entityManager.flush();

        var first = orchestrator.enqueueBatch(user, 0, 25);
        entityManager.flush();

        assertEquals(2, first.repositoriesConsidered());
        assertEquals(2, first.jobsQueued());
        assertEquals(0, first.alreadyQueued());

        var second = orchestrator.enqueueBatch(user, 0, 25);
        entityManager.flush();

        assertEquals(2, second.repositoriesConsidered());
        assertEquals(0, second.jobsQueued());
        assertEquals(2, second.alreadyQueued());
    }
}
