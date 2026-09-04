package io.github.developeranalytics.domain.model;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class SourceRepositoryAnalysisFreshnessTest {

    @Test
    void repositoryNeedsInitialAnalysisAndBecomesFreshWhenCompleted() {
        SourceRepository repository = new SourceRepository(null, "github", "1", "owner", "repo");
        OffsetDateTime activity = OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        repository.setLastActivityAt(activity);

        assertTrue(repository.needsAnalysisRefresh());

        repository.markAnalysisCompleted(activity.plusMinutes(5));

        assertFalse(repository.needsAnalysisRefresh());
    }

    @Test
    void newerRepositoryActivityMakesCompletedAnalysisStale() {
        SourceRepository repository = new SourceRepository(null, "github", "1", "owner", "repo");
        OffsetDateTime activity = OffsetDateTime.of(2026, 9, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        repository.setLastActivityAt(activity);
        repository.markAnalysisCompleted(activity.plusMinutes(5));

        repository.setLastActivityAt(activity.plusHours(2));

        assertTrue(repository.needsAnalysisRefresh());
    }

    @Test
    void repositoryWithoutActivityCanStillBeFreshAfterAnalysis() {
        SourceRepository repository = new SourceRepository(null, "github", "1", "owner", "empty-repo");

        repository.markAnalysisCompleted(OffsetDateTime.now(ZoneOffset.UTC));

        assertFalse(repository.needsAnalysisRefresh());
    }
}
