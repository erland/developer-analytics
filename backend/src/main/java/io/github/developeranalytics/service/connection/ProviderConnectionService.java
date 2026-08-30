package io.github.developeranalytics.service.connection;

import io.github.developeranalytics.domain.model.ProviderConnection;
import io.github.developeranalytics.persistence.auth.ProviderConnectionRepository;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@ApplicationScoped
public class ProviderConnectionService {

    @Inject
    ProviderConnectionRepository repository;

    @Inject
    ProviderCredentialService credentials;

    @Inject
    BackgroundJobRepository backgroundJobs;

    @Inject
    ProviderDisconnectDataService disconnectData;

    public List<ProviderConnection> list(UUID userId) {
        return repository.findAllForUser(userId);
    }

    public ProviderConnection get(UUID userId, String provider) {
        return repository.findForUserAndProvider(userId, normalize(provider))
                .orElseThrow(NotFoundException::new);
    }


@Transactional
public DisconnectResult disconnect(
        UUID userId,
        String provider,
        DisconnectDataDisposition dataDisposition
) {
    String normalizedProvider = normalize(provider);
    ProviderConnection connection = get(
            userId,
            normalizedProvider
    );

    OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

    int cancelledJobs = backgroundJobs.cancelProviderJobs(
            userId,
            normalizedProvider,
            now
    );

    credentials.removeCredential(
            userId,
            normalizedProvider
    );
    connection.removePrivateRepositoryAccess();
    connection.disconnect();

    ProviderDisconnectDataService.RemovalSummary removal =
            dataDisposition ==
                    DisconnectDataDisposition.REMOVE_ANALYSED_DATA
                    ? disconnectData.removeAnalysedProviderData(
                            userId,
                            normalizedProvider
                    )
                    : null;

    return new DisconnectResult(
            connection,
            dataDisposition,
            cancelledJobs,
            removal != null,
            removal
    );
}

public record DisconnectResult(
        ProviderConnection connection,
        DisconnectDataDisposition dataDisposition,
        int cancelledBackgroundJobs,
        boolean analysedDataRemoved,
        ProviderDisconnectDataService.RemovalSummary removalSummary
) {}

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
