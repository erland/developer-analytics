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
        String source = Files.readString(Path.of(
                "src/main/java/io/github/developeranalytics/api/MeActivityResource.java"
        ));

        assertFalse(source.contains(":fromDate is null"));
        assertFalse(source.contains(":toDate is null"));
        assertTrue(source.contains("if (fromDate != null)"));
        assertTrue(source.contains("if (toDate != null)"));
        assertTrue(source.contains("projectInventory.find"));
        assertTrue(source.contains("c.repository.id in :repositoryIds"));
        assertTrue(source.contains("matchingRepositoryIds.contains(repositoryId)"));
        assertTrue(source.contains("AnalysisPeriod.resolve(from, to, year, month, week)"));
    }
}
