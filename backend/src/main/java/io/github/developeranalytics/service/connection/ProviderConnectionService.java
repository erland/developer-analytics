package io.github.developeranalytics.service.connection;

import io.github.developeranalytics.domain.model.ProviderConnection;
import io.github.developeranalytics.persistence.auth.ProviderConnectionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class ProviderConnectionService {

    @Inject
    ProviderConnectionRepository repository;

    @Inject
    ProviderCredentialService credentials;

    public List<ProviderConnection> list(UUID userId) {
        return repository.findAllForUser(userId);
    }

    public ProviderConnection get(UUID userId, String provider) {
        return repository.findForUserAndProvider(userId, normalize(provider))
                .orElseThrow(NotFoundException::new);
    }

    @Transactional
    public ProviderConnection disconnect(UUID userId, String provider) {
        ProviderConnection connection = get(userId, provider);
        credentials.removeCredential(userId, provider);
        connection.disconnect();
        return connection;
    }

    @Transactional
    public ProviderConnection markValidated(UUID userId, String provider) {
        ProviderConnection connection = get(userId, provider);
        connection.markValidated();
        return connection;
    }

    private String normalize(String provider) {
        return provider.trim().toLowerCase(Locale.ROOT);
    }
}
