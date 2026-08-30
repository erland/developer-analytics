package io.github.developeranalytics.ai;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class AiOutageRecoveryContractTest {

    @Test
    void gatewayKeepsOptionalResultContractForAllAiOperations() {
        for (String methodName : new String[] {
                "classifyProject",
                "summariseProject",
                "normaliseTechnologies",
                "inferRoles",
                "summariseUserInsights",
                "summariseTechnologyHistory"
        }) {
            Method method = java.util.Arrays.stream(
                    AiAnalysisGateway.class.getDeclaredMethods()
            )
            .filter(candidate -> candidate.getName().equals(methodName))
            .findFirst()
            .orElseThrow();

            assertEquals(
                    java.util.Optional.class,
                    method.getReturnType(),
                    methodName
            );
        }
    }
}
