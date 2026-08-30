package io.github.developeranalytics.service.connection;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.ProviderConnection;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProviderCredentialDisconnectedTest {

    @Test
    void domainConnectionRemovesPrivateAccessOnExplicitDisconnectWorkflow() {
        ProviderConnection connection =
                new ProviderConnection(AppUser.create(), null, "github");
        connection.authorisePrivateRepositoryAccess();

        connection.removePrivateRepositoryAccess();
        connection.clearCredential();
        connection.disconnect();

        assertFalse(connection.isPrivateRepositoryAccessAuthorised());
        assertEquals(
                ProviderConnection.Status.DISCONNECTED.name(),
                connection.getStatus()
        );
        assertNull(connection.getCredentialCiphertext());
    }
}
