package io.github.developeranalytics.service.connection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DisconnectDataDispositionTest {

    @Test
    void disconnectRequiresExplicitPreserveOrRemoveChoice() {
        assertArrayEquals(
                new DisconnectDataDisposition[] {
                        DisconnectDataDisposition.PRESERVE_ANALYSED_DATA,
                        DisconnectDataDisposition.REMOVE_ANALYSED_DATA
                },
                DisconnectDataDisposition.values()
        );
    }
}
