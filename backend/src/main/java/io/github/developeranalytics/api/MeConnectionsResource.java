package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.model.ProviderConnection;
import io.github.developeranalytics.domain.model.ProviderIdentity;
import io.github.developeranalytics.service.connection.ProviderConnectionService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/me/connections")
@Produces(MediaType.APPLICATION_JSON)
public class MeConnectionsResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    ProviderConnectionService connections;

    @GET
    public List<ConnectionSummary> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        return connections.list(current.user().getId()).stream()
                .map(ConnectionSummary::from)
                .toList();
    }

    @GET
    @Path("/{provider}")
    public ConnectionSummary get(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("provider") String provider
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        return ConnectionSummary.from(connections.get(current.user().getId(), provider));
    }

    @POST
    @Path("/{provider}/disconnect")
    public ConnectionSummary disconnect(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("provider") String provider
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        return ConnectionSummary.from(connections.disconnect(current.user().getId(), provider));
    }

    @POST
    @Path("/{provider}/validate")
    public ConnectionSummary validate(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("provider") String provider
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        return ConnectionSummary.from(connections.markValidated(current.user().getId(), provider));
    }

    public record ConnectionSummary(
            UUID id,
            String provider,
            String status,
            String login,
            OffsetDateTime connectedAt,
            OffsetDateTime lastValidatedAt,
            OffsetDateTime disconnectedAt
    ) {
        static ConnectionSummary from(ProviderConnection connection) {
            ProviderIdentity identity = connection.getProviderIdentity();
            return new ConnectionSummary(
                    connection.getId(),
                    connection.getProvider(),
                    connection.getStatus(),
                    identity == null ? null : identity.getLogin(),
                    connection.getConnectedAt(),
                    connection.getLastValidatedAt(),
                    connection.getDisconnectedAt()
            );
        }
    }
}
