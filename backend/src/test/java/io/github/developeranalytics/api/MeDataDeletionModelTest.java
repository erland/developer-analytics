package io.github.developeranalytics.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MeDataDeletionModelTest {

    @Test
    void destructiveEndpointRequiresLiteralExplicitConfirmation() {
        assertEquals(
                "DELETE_MY_DATA",
                MeDataDeletionResource.CONFIRMATION
        );

        var request =
                new MeDataDeletionResource.DeleteRequest("DELETE_MY_DATA");
        assertEquals("DELETE_MY_DATA", request.confirmation());
    }
}
