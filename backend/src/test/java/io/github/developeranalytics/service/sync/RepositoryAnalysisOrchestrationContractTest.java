package io.github.developeranalytics.service.sync;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class RepositoryAnalysisOrchestrationContractTest {

    @Test
    void repositoryDiscoveryQueuesCompleteAnalysisInsteadOfOnlyFirstBatch() throws Exception {
        String handler = Files.readString(Path.of(
                "src/main/java/io/github/developeranalytics/worker/GitHubRepositoryDiscoveryJobHandler.java"));
        String orchestrator = Files.readString(Path.of(
                "src/main/java/io/github/developeranalytics/service/sync/RepositoryAnalysisOrchestrator.java"));

        assertTrue(handler.contains("analysis.enqueueAll(job.getUser())"));
        assertFalse(handler.contains("DEFAULT_BATCH_SIZE"));
        assertTrue(orchestrator.contains("enqueueContributionDiscovery"));
        assertTrue(orchestrator.contains("enqueueLanguageEvidence"));
        assertTrue(orchestrator.contains("enqueueFileManifestEvidence"));
        assertTrue(orchestrator.contains("enqueueDeterministicClassification"));
        assertTrue(orchestrator.contains("enqueueTechnologyAssessmentRecalculation"));
        assertTrue(orchestrator.contains("enqueueTechnologyTimelineRecalculation"));
        assertTrue(orchestrator.contains("enqueueProjectSignificanceRecalculation"));
    }
}
