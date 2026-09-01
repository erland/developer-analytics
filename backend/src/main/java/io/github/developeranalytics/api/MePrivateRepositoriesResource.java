package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.service.job.RepositoryDiscoveryJobService;
import io.github.developeranalytics.service.sync.RepositoryAnalysisOrchestrator;
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
    @Inject RepositoryAnalysisOrchestrator analysis;

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
        if (selection.included()) {
            repository.includeInAnalysis();
            analysis.enqueueRepository(current.user(), repository.getId());
        } else {
            repository.excludeFromAnalysis();
            analysis.enqueueAggregateJobs(current.user());
        }
        return Item.from(repository);
    }


    @PUT
    @Path("/selection")
    @Transactional
    public BulkSelectionResponse bulkSelect(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String token,
            BulkSelection selection
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(token);
        if (selection == null) {
            throw new BadRequestException("Selection is required");
        }

        String prefix = selection.prefix() == null
                ? ""
                : selection.prefix().trim().toLowerCase(java.util.Locale.ROOT);

        List<SourceRepository> matched = repositories
                .findPrivateForUser(current.user().getId())
                .stream()
                .filter(repository -> prefix.isBlank()
                        || repository.getName().toLowerCase(java.util.Locale.ROOT).startsWith(prefix)
                        || (repository.getFullName() != null
                            && repository.getFullName().toLowerCase(java.util.Locale.ROOT).startsWith(prefix)))
                .toList();

        int analysisQueuedFor = 0;
        for (SourceRepository repository : matched) {
            if (selection.included()) {
                repository.includeInAnalysis();
                analysis.enqueueRepository(current.user(), repository.getId());
                analysisQueuedFor++;
            } else {
                repository.excludeFromAnalysis();
            }
        }
        if (!selection.included() && !matched.isEmpty()) {
            analysis.enqueueAggregateJobs(current.user());
        }

        return new BulkSelectionResponse(
                matched.size(),
                selection.included(),
                prefix.isBlank() ? null : prefix,
                analysisQueuedFor
        );
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
        analysis.enqueueAggregateJobs(current.user());
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
    public record BulkSelection(boolean included, String prefix) {}
    public record BulkSelectionResponse(
            int repositoriesMatched,
            boolean included,
            String prefix,
            int analysisQueuedFor
    ) {}
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
