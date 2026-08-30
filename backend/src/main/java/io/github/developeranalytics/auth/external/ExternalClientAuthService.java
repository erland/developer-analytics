package io.github.developeranalytics.auth.external;

import io.github.developeranalytics.auth.CryptoTokens;
import io.github.developeranalytics.domain.external.ExternalClientToken;
import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.persistence.external.ExternalClientTokenRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;

@ApplicationScoped
public class ExternalClientAuthService {

    @Inject
    ExternalClientTokenRepository tokens;

    @Transactional
    public ExternalClientPrincipal require(
            String authorizationHeader,
            ExternalClientToken.Scope requiredScope
    ) {
        String rawToken = bearerToken(authorizationHeader);

        ExternalClientToken token = tokens
                .findActiveByHash(CryptoTokens.sha256(rawToken))
                .orElseThrow(() ->
                        new NotAuthorizedException(
                                "External client authentication required"
                        ));

        if (!token.hasScope(requiredScope)) {
            throw new ForbiddenException(
                    "External client token lacks required scope: " +
                            requiredScope
            );
        }

        token.markUsed();
        return new ExternalClientPrincipal(
                token.getUser(),
                token.getId(),
                token.getName(),
                token.getPrivacyScope()
        );
    }

    public record ExternalClientPrincipal(
            AppUser user,
            java.util.UUID tokenId,
            String clientName,
            ExternalClientToken.PrivacyScope privacyScope
    ) {}

    private String bearerToken(String header) {
        if (header == null ||
                !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            throw new NotAuthorizedException(
                    "Bearer token required"
            );
        }

        String token = header.substring(7).trim();
        if (token.isBlank()) {
            throw new NotAuthorizedException(
                    "Bearer token required"
            );
        }
        return token;
    }
}
