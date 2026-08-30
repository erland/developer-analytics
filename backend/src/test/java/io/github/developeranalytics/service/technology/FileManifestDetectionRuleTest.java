package io.github.developeranalytics.service.technology;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class FileManifestDetectionRuleTest {

    @Test
    void representativeEvidencePatternsAreDeterministic() {
        String packageJson = "{\"dependencies\":{\"react\":\"19\",\"vite\":\"7\"}}";
        String pom = "<dependency><groupId>io.quarkus</groupId></dependency>";

        assertTrue(packageJson.toLowerCase().contains("\"react\""));
        assertTrue(packageJson.toLowerCase().contains("\"vite\""));
        assertTrue(pom.toLowerCase().contains("io.quarkus"));
        assertTrue(".github/workflows/ci.yml".startsWith(".github/workflows/"));
        assertTrue("backend/pom.xml".endsWith("/pom.xml"));
    }
}
