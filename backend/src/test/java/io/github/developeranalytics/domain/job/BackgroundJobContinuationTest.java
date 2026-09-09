package io.github.developeranalytics.domain.job;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class BackgroundJobContinuationTest {

    @Test
    void continuationCursorCanBeCheckpointedAndCleared() {
        OffsetDateTime now = OffsetDateTime.of(2026, 9, 9, 15, 0, 0, 0, ZoneOffset.UTC);
        BackgroundJob job = BackgroundJob.queued(
                null,
                "GITHUB_CONTRIBUTION_DISCOVERY",
                100,
                Map.of("repositoryId", "repo-1", "contributionCursor", "4"),
                5,
                now
        );

        job.putPayloadValue("contributionCursor", "5");
        assertEquals("5", job.getPayload().get("contributionCursor"));

        job.putPayloadValue("contributionCursor", null);
        assertFalse(job.getPayload().containsKey("contributionCursor"));
        assertEquals("repo-1", job.getPayload().get("repositoryId"));
    }

    @Test
    void checkpointPayloadRemainsMutableWhenOriginalPayloadWasImmutable() {
        BackgroundJob job = BackgroundJob.queued(
                null,
                "GITHUB_CONTRIBUTION_DISCOVERY",
                100,
                Map.of("repositoryId", "repo-1"),
                5,
                OffsetDateTime.now(ZoneOffset.UTC)
        );

        assertDoesNotThrow(() -> job.putPayloadValue("contributionCursor", "2"));
        assertEquals("2", job.getPayload().get("contributionCursor"));
    }
}
