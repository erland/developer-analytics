package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CryptoTokens;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.external.ExternalClientToken;
import io.github.developeranalytics.persistence.external.ExternalClientTokenRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Path("/api/me/external-clients")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MeExternalClientsResource {

    @Inject CurrentUserService currentUserService;
    @Inject ExternalClientTokenRepository tokens;

    @GET
    public List<TokenInfo> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        return tokens.findForUser(current.user().getId())
                .stream()
                .map(TokenInfo::from)
                .toList();
    }

    @POST
    @Transactional
    public CreatedToken create(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            CreateRequest request
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        if (request == null ||
                request.name() == null ||
                request.name().isBlank() ||
                request.scopes() == null ||
                request.scopes().isEmpty() ||
                request.privacyScope() == null) {
            throw new BadRequestException(
                    "External client name and at least one scope are required"
            );
        }

        String rawToken = "da_ext_" + CryptoTokens.randomUrlToken(48);

        ExternalClientToken token =
                new ExternalClientToken(
                        current.user(),
                        request.name(),
                        CryptoTokens.sha256(rawToken),
                        request.scopes(),
                        request.privacyScope()
                );
        tokens.persist(token);

        // The raw credential is returned exactly once and is never stored.
        return new CreatedToken(
                token.getId(),
                token.getName(),
                token.getScopes(),
                token.getPrivacyScope(),
                token.getCreatedAt(),
                rawToken
        );
    }

    @DELETE
    @Path("/{tokenId}")
    @Transactional
    public TokenInfo revoke(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("tokenId") UUID tokenId
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        ExternalClientToken token = tokens
                .findByIdForUser(
                        tokenId,
                        current.user().getId()
                )
                .orElseThrow(NotFoundException::new);

        token.revoke();
        return TokenInfo.from(token);
    }

    public record CreateRequest(
            String name,
            Set<ExternalClientToken.Scope> scopes,
            ExternalClientToken.PrivacyScope privacyScope
    ) {}

    public record CreatedToken(
            UUID id,
            String name,
            Set<ExternalClientToken.Scope> scopes,
            ExternalClientToken.PrivacyScope privacyScope,
            OffsetDateTime createdAt,
            String token
    ) {}

    public record TokenInfo(
            UUID id,
            String name,
            Set<ExternalClientToken.Scope> scopes,
            ExternalClientToken.PrivacyScope privacyScope,
            OffsetDateTime createdAt,
            OffsetDateTime lastUsedAt,
            OffsetDateTime revokedAt
    ) {
        static TokenInfo from(ExternalClientToken token) {
            return new TokenInfo(
                    token.getId(),
                    token.getName(),
                    token.getScopes(),
                    token.getPrivacyScope(),
                    token.getCreatedAt(),
                    token.getLastUsedAt(),
                    token.getRevokedAt()
            );
        }
    }
}
