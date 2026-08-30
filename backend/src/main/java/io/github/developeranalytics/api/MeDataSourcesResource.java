package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.model.ProviderConnection;
import io.github.developeranalytics.persistence.auth.ProviderConnectionRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;

@Path("/api/me/data-sources")
@Produces(MediaType.APPLICATION_JSON)
public class MeDataSourcesResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    ProviderConnectionRepository connections;

    @GET
    @Path("/github")
    public GitHubStatus github(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(token);

        ProviderConnection connection = connections
                .findForUserAndProvider(
                        current.user().getId(),
                        "github"
                )
                .orElseThrow();

        return new GitHubStatus(
                connection.getStatus(),
                connection.isPrivateRepositoryAccessAuthorised(),
                connection.getPrivateRepositoryAuthorisedAt()
        );
    }

    public record GitHubStatus(
            String status,
            boolean privateRepositoriesAuthorised,
            OffsetDateTime privateRepositoriesAuthorisedAt
    ) {}
}
