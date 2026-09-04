package io.github.developeranalytics.service.technology;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class FileManifestDetectionRuleTest {

    @Test
    void representativeEvidencePatternsAreDeterministic() {
        FileManifestEvidenceService service = new FileManifestEvidenceService();

        assertTrue(service.matchesFilePattern("androidmanifest.xml", "AndroidManifest.xml"));
        assertTrue(service.matchesFilePattern("rssphotoshow.xcodeproj/project.pbxproj", "project.pbxproj"));
        assertTrue(service.matchesFilePattern("rfidremote/rfidremote.ino", "*.ino"));
        assertTrue(service.matchesFilePattern("firmware/platformio.ini", "platformio.ini"));
        assertFalse(service.matchesFilePattern("src/main.cpp", "*.ino"));

        String packageJson = "{\"dependencies\":{\"react\":\"19\",\"vite\":\"7\"}}";
        String pom = "<dependency><groupId>io.quarkus</groupId></dependency>";
        assertTrue(packageJson.toLowerCase().contains("\"react\""));
        assertTrue(packageJson.toLowerCase().contains("\"vite\""));
        assertTrue(pom.toLowerCase().contains("io.quarkus"));
    }
}
