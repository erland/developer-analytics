package io.github.developeranalytics.provider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class RepositorySnapshotFilePolicyTest {

    @Test
    void recognizesTechnologyAndManifestEvidenceFiles() {
        assertTrue(RepositorySnapshotFilePolicy.isRelevantTechnologyFile("pom.xml"));
        assertTrue(RepositorySnapshotFilePolicy.isRelevantTechnologyFile("frontend/package.json"));
        assertTrue(RepositorySnapshotFilePolicy.isRelevantTechnologyFile(".github/workflows/ci.yml"));
        assertTrue(RepositorySnapshotFilePolicy.isRelevantTechnologyFile("service/db/migration/V1__init.sql"));
        assertTrue(RepositorySnapshotFilePolicy.isRelevantTechnologyFile("infra/main.tf"));
        assertFalse(RepositorySnapshotFilePolicy.isRelevantTechnologyFile("src/main/java/App.java"));
        assertFalse(RepositorySnapshotFilePolicy.isRelevantTechnologyFile(null));
    }

    @Test
    void selectionIsBoundedAndPreservesRepositoryOrder() {
        List<String> paths = new ArrayList<>();
        paths.add("src/App.java");
        for (int i = 0; i < 60; i++) paths.add("modules/" + i + "/package.json");

        List<String> selected = RepositorySnapshotFilePolicy.selectRelevantPaths(paths);

        assertEquals(RepositorySnapshotFilePolicy.MAX_RELEVANT_FILES, selected.size());
        assertEquals("modules/0/package.json", selected.getFirst());
        assertEquals("modules/39/package.json", selected.getLast());
    }

    @Test
    void fileSizeBoundIsSharedAndStrict() {
        assertFalse(RepositorySnapshotFilePolicy.isReadableFileSize(-1));
        assertTrue(RepositorySnapshotFilePolicy.isReadableFileSize(0));
        assertTrue(RepositorySnapshotFilePolicy.isReadableFileSize(RepositorySnapshotFilePolicy.MAX_FILE_BYTES));
        assertFalse(RepositorySnapshotFilePolicy.isReadableFileSize(
                RepositorySnapshotFilePolicy.MAX_FILE_BYTES + 1));
    }
}
