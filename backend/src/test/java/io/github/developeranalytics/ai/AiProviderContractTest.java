package io.github.developeranalytics.ai;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class AiProviderContractTest {

    @Test
    void vendorNeutralContractContainsInitialOperations() {
        Set<String> methods = java.util.Arrays.stream(
                AiProvider.class.getDeclaredMethods()
        )
        .map(Method::getName)
        .collect(Collectors.toSet());

        assertTrue(methods.contains("classifyProject"));
        assertTrue(methods.contains("summariseProject"));
        assertTrue(methods.contains("normaliseTechnologies"));
        assertTrue(methods.contains("inferRoles"));
        assertTrue(methods.contains("summariseTechnologyHistory"));
        assertTrue(methods.contains("summariseUserInsights"));
    }
}
