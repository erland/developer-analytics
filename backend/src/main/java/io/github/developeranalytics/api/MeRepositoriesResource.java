package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

@Path("/api/me/repositories")
@Produces(MediaType.APPLICATION_JSON)
public class MeRepositoriesResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    SourceRepositoryRepository repositories;

    @GET
    public List<RepositorySummary> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);

        return repositories.findAllForUser(current.user().getId())
                .stream()
                .map(RepositorySummary::from)
                .toList();
    }

    @GET
    @Path("/{repositoryId}")
    public RepositorySummary get(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("repositoryId") UUID repositoryId
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);

        SourceRepository repository = repositories
                .findByIdForUser(repositoryId, current.user().getId())
                .orElseThrow(NotFoundException::new);

        return RepositorySummary.from(repository);
    }

    public record RepositorySummary(
            UUID id,
            String provider,
            String externalRepositoryId,
            String name
    ) {
        static RepositorySummary from(SourceRepository repository) {
            return new RepositorySummary(
                    repository.getId(),
                    repository.getProvider(),
                    repository.getExternalRepositoryId(),
                    repository.getName()
            );
        }
    }
}
