package io.github.developeranalytics.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class MeActivityResourceQueryTest {

    @Test
    void allTimeQueryDoesNotBindUntypedNullDateParameters() throws Exception {
        String resourceSource = Files.readString(Path.of(
                "src/main/java/io/github/developeranalytics/api/MeActivityResource.java"
        ));
        String serviceSource = Files.readString(Path.of(
                "src/main/java/io/github/developeranalytics/service/activity/ActivityApplicationService.java"
        ));

        assertFalse(serviceSource.contains(":fromDate is null"));
        assertFalse(serviceSource.contains(":toDate is null"));
        assertTrue(serviceSource.contains("if (fromDate != null)"));
        assertTrue(serviceSource.contains("if (toDate != null)"));
        assertTrue(serviceSource.contains("projectInventory.find"));
        assertTrue(serviceSource.contains("c.repository.id in :repositoryIds"));
        assertTrue(serviceSource.contains("matchingRepositoryIds.contains(repositoryId)"));
        assertTrue(resourceSource.contains("AnalysisPeriod.resolve(from, to, year, month, week)"));
    }
}
