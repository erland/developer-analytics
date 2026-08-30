package io.github.developeranalytics.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProviderConnectionPrivateAccessTest {

    @Test
    void privateRepositoryAccessStartsDisabledAndRequiresExplicitAuthorisation() {
        AppUser user = AppUser.create();
        ProviderConnection connection =
                new ProviderConnection(user, null, "github");

        assertFalse(connection.isPrivateRepositoryAccessAuthorised());
        assertEquals(
                ProviderConnection.PrivateRepositoryAccess.NOT_AUTHORISED,
                connection.getPrivateRepositoryAccess()
        );

        connection.authorisePrivateRepositoryAccess();

        assertTrue(connection.isPrivateRepositoryAccessAuthorised());
        assertNotNull(connection.getPrivateRepositoryAuthorisedAt());

        connection.removePrivateRepositoryAccess();

        assertFalse(connection.isPrivateRepositoryAccessAuthorised());
        assertNull(connection.getPrivateRepositoryAuthorisedAt());
    }
}
