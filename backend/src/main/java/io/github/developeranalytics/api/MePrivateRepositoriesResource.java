package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.UUID;

@Path("/api/me/private-repositories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MePrivateRepositoriesResource {

    @Inject CurrentUserService currentUserService;
    @Inject SourceRepositoryRepository repositories;
    @Inject RepositoryDiscoveryJobService jobs;

    @GET
    public List<Item> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        return repositories.findPrivateForUser(current.user().getId())
                .stream().map(Item::from).toList();
    }

    @PUT
    @Path("/{repositoryId}/selection")
    @Transactional
    public Item select(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            @PathParam("repositoryId") UUID repositoryId,
            Selection selection
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        SourceRepository repository = requirePrivateRepository(
                current.user().getId(), repositoryId);
        if (selection.included()) repository.includeInAnalysis();
        else repository.excludeFromAnalysis();
        return Item.from(repository);
    }

    @POST
    @Path("/refresh")
    public RefreshResponse refresh(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        BackgroundJob job = jobs.enqueueGitHubDiscovery(current.user());
        return new RefreshResponse(job.getId());
    }

    @DELETE
    @Path("/{repositoryId}")
    @Transactional
    public Item removeFromAnalysis(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            @PathParam("repositoryId") UUID repositoryId
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        SourceRepository repository = requirePrivateRepository(
                current.user().getId(), repositoryId);
        repository.excludeFromAnalysis();
        return Item.from(repository);
    }

    private SourceRepository requirePrivateRepository(UUID userId, UUID repositoryId) {
        SourceRepository repository = repositories
                .findByIdForUser(repositoryId, userId)
                .orElseThrow(NotFoundException::new);
        if (repository.getVisibility() != RepositoryVisibility.PRIVATE) {
            throw new BadRequestException("Only private repositories are managed here");
        }
        return repository;
    }

    public record Selection(boolean included) {}
    public record RefreshResponse(UUID jobId) {}
    public record Item(
            UUID id,
            String name,
            String fullName,
            String htmlUrl,
            boolean includedInAnalysis,
            String syncStatus
    ) {
        static Item from(SourceRepository r) {
            return new Item(r.getId(), r.getName(), r.getFullName(), r.getHtmlUrl(),
                    r.isIncludedInAnalysis(), r.getSyncStatus().name());
        }
    }
}
