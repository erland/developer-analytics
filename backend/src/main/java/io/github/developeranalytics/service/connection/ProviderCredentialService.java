package io.github.developeranalytics.service.connection;

import io.github.developeranalytics.domain.model.ProviderConnection;
import io.github.developeranalytics.persistence.auth.ProviderConnectionRepository;
import io.github.developeranalytics.provider.ProviderAccessToken;
import io.github.developeranalytics.security.CredentialCipher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class ProviderCredentialService {

    @Inject
    ProviderConnectionRepository connections;

    @Inject
    CredentialCipher cipher;

    @Transactional
    public void storeAccessToken(UUID userId, String provider, String accessToken) {
        ProviderConnection connection = connections
                .findForUserAndProvider(userId, normalize(provider))
                .orElseThrow(NotFoundException::new);

        CredentialCipher.EncryptedValue encrypted = cipher.encrypt(accessToken);
        connection.setEncryptedCredential(
                encrypted.ciphertext(),
                encrypted.keyVersion()
        );
        connection.markValidated();
    }

    public ProviderAccessToken requireAccessToken(UUID userId, String provider) {
        ProviderConnection connection = connections
                .findForUserAndProvider(userId, normalize(provider))
                .orElseThrow(NotFoundException::new);

        if (connection.getCredentialCiphertext() == null ||
                connection.getCredentialKeyVersion() == null) {
            throw new IllegalStateException(
                    "No stored credential is available for provider " + provider);
        }

        return new ProviderAccessToken(
                cipher.decrypt(
                        connection.getCredentialCiphertext(),
                        connection.getCredentialKeyVersion()
                )
        );
    }

    @Transactional
    public void removeCredential(UUID userId, String provider) {
        ProviderConnection connection = connections
                .findForUserAndProvider(userId, normalize(provider))
                .orElseThrow(NotFoundException::new);
        connection.clearCredential();
    }

    private String normalize(String provider) {
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
