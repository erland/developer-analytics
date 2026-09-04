package io.github.developeranalytics.service.technology;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class BuiltInTechnologyCoverageTest {

    @Test
    void catalogueCoversRepresentativeLegacyAndPlatformTechnologies() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("technology-catalogue.json")) {
            JsonNode root = mapper.readTree(in);
            Set<String> keys = new HashSet<>();
            root.path("technologies").forEach(node -> keys.add(node.path("key").asText()));

            assertTrue(keys.containsAll(Set.of(
                    "perl", "lua", "c", "cpp", "objective-c",
                    "android", "ios", "arduino")));
        }
    }

    @Test
    void projectTaxonomyContainsEmbeddedIotWithoutSplittingAndroidIntoItsOwnProjectType() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("project-category-taxonomy.json")) {
            JsonNode root = mapper.readTree(in);
            Set<String> keys = new HashSet<>();
            root.path("categories").forEach(node -> keys.add(node.path("key").asText()));

            assertTrue(keys.contains("mobile-application"));
            assertTrue(keys.contains("embedded-iot"));
            assertTrue(!keys.contains("android-application"));
        }
    }
}
