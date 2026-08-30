package io.github.developeranalytics.health;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BackendLivenessCheckTest {

    @Test
    void backendProcessIsLiveWhenCheckExecutes() {
        var response = new BackendLivenessCheck().call();
        assertEquals("UP", response.getStatus().name());
    }
}
