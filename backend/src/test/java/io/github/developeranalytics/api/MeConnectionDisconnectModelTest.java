package io.github.developeranalytics.api;

import io.github.developeranalytics.service.connection.DisconnectDataDisposition;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("privacy")
@Tag("unit")
class MeConnectionDisconnectModelTest {

    @Test
    void requestCarriesExplicitDataDisposition() {
        var request = new MeConnectionsResource.DisconnectRequest(
                DisconnectDataDisposition.REMOVE_ANALYSED_DATA
        );

        assertEquals(
                DisconnectDataDisposition.REMOVE_ANALYSED_DATA,
                request.dataDisposition()
        );
    }
}
