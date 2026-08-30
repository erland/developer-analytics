package io.github.developeranalytics.health;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class BackendLivenessCheckTest {

    @Test
    void backendProcessIsLiveWhenCheckExecutes() {
        var response = new BackendLivenessCheck().call();
        assertEquals("UP", response.getStatus().name());
    }
}
