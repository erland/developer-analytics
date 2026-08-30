package io.github.developeranalytics.auth;

import io.github.developeranalytics.domain.auth.UserSession;
import io.github.developeranalytics.domain.model.ProviderIdentity;
import io.github.developeranalytics.persistence.auth.AuthenticationRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotAuthorizedException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.OffsetDateTime;

@RequestScoped
public class CurrentUserService {

    @Inject
    AuthenticationRepository authenticationRepository;

    public CurrentUser requireCurrentUser(String rawSessionToken) {
        if (rawSessionToken == null || rawSessionToken.isBlank()) {
            throw new NotAuthorizedException("Authentication required");
        }

        UserSession session = authenticationRepository
                .findValidSession(CryptoTokens.sha256(rawSessionToken), OffsetDateTime.now())
                .orElseThrow(() -> new NotAuthorizedException("Authentication required"));

        ProviderIdentity identity = authenticationRepository
                .findGitHubIdentityForUser(session.getUser().getId())
                .orElseThrow(() -> new NotAuthorizedException("Authenticated identity is unavailable"));

        session.touch();
        return new CurrentUser(session.getUser(), identity);
    }
}
