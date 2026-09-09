package io.github.developeranalytics.api;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("privacy")
@Tag("unit")
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

    @Test
    void deletionFailureHasStableRetryableApiContract() {
        assertEquals(
                "DATA_DELETION_FAILED",
                MeDataDeletionResource.DELETION_FAILED_CODE
        );

        var response = new MeDataDeletionResource.DeleteFailureResponse(
                false,
                MeDataDeletionResource.DELETION_FAILED_CODE,
                "retry",
                true
        );

        assertFalse(response.deleted());
        assertTrue(response.retryable());
        assertEquals("DATA_DELETION_FAILED", response.code());
    }
}
