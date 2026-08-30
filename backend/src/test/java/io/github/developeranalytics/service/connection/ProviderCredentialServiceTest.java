package io.github.developeranalytics.service.connection;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.ProviderConnection;
import io.github.developeranalytics.domain.model.ProviderIdentity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@Tag("persistence")
@Tag("privacy")
class ProviderCredentialServiceTest {

    @Inject
    EntityManager entityManager;

    @Inject
    ProviderCredentialService credentials;

    @Test
    @Transactional
    void storesOnlyEncryptedCredentialAndReturnsRedactedAccessTokenObject() {
        AppUser user = AppUser.create();
        entityManager.persist(user);

        ProviderIdentity identity = new ProviderIdentity(
                user, "github", "3001", "alice", "Alice");
        entityManager.persist(identity);

        ProviderConnection connection = new ProviderConnection(
                user, identity, "github");
        entityManager.persist(connection);
        entityManager.flush();

        credentials.storeAccessToken(
                user.getId(),
                "github",
                "real-github-token"
        );

        entityManager.flush();

        assertNotNull(connection.getCredentialCiphertext());
        assertFalse(connection.getCredentialCiphertext().contains("real-github-token"));
        assertEquals("test-v1", connection.getCredentialKeyVersion());

        var token = credentials.requireAccessToken(user.getId(), "github");

        assertEquals("real-github-token", token.value());
        assertEquals("[REDACTED]", token.toString());
    }
}
