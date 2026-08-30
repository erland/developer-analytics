package io.github.developeranalytics.api.external;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ExternalAnalysisContractTest {

    @Test
    void contractUsesExplicitVersionedMediaType() {
        assertEquals(
                "application/vnd.developer-analytics.analysis.v1+json",
                ExternalAnalysisMediaType.VALUE
        );
    }

    @Test
    void definesRequiredCompactMeEndpoints() {
        Set<String> paths = java.util.Arrays.stream(
                ExternalAnalysisResource.class.getDeclaredMethods()
        )
        .filter(method -> method.isAnnotationPresent(GET.class))
        .map(method -> {
            Path path = method.getAnnotation(Path.class);
            return path == null ? "" : path.value();
        })
        .collect(Collectors.toSet());

        assertTrue(paths.contains("/profile"));
        assertTrue(paths.contains("/projects"));
        assertTrue(paths.contains("/activity"));
        assertTrue(paths.contains("/technologies"));
        assertTrue(paths.contains("/project-types"));
        assertTrue(paths.contains("/contributions"));
        assertTrue(paths.contains("/evidence"));
    }

    @Test
    void projectContractExcludesFrontendAndSourcePayloadFields() {
        Set<String> fields = java.util.Arrays.stream(
                ExternalAnalysisResource.Project.class.getRecordComponents()
        )
        .map(component -> component.getName())
        .collect(Collectors.toSet());

        assertFalse(fields.contains("description"));
        assertFalse(fields.contains("htmlUrl"));
        assertFalse(fields.contains("sourceContent"));
        assertFalse(fields.contains("chart"));
        assertTrue(fields.contains("projectTypes"));
        assertTrue(fields.contains("technologies"));
    }
}
