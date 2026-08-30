package io.github.developeranalytics.ai;

import java.util.UUID;

public record AiRequestContext(
        UUID userId,
        AiDataSensitivity sensitivity
) {
    public AiRequestContext {
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        if (sensitivity == null) {
            throw new IllegalArgumentException("sensitivity is required");
        }
    }
}
