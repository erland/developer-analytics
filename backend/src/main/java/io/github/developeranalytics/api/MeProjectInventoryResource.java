package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.project.ProjectInventoryRepository;
import io.github.developeranalytics.persistence.project.RepositoryProjectCategoryRepository;
import io.github.developeranalytics.persistence.technology.RepositoryTechnologyEvidenceRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/me/project-inventory")
@Produces(MediaType.APPLICATION_JSON)
public class MeProjectInventoryResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    ProjectInventoryRepository inventory;

    @Inject
    RepositoryProjectCategoryRepository categories;

    @Inject
    RepositoryTechnologyEvidenceRepository technologies;

    @GET
    public Response list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("pageSize") @DefaultValue("25") int pageSize,
            @QueryParam("search") String search,
            @QueryParam("ownership") String ownership,
            @QueryParam("visibility") String visibility,
            @QueryParam("activity") String activity,
            @QueryParam("category") String category,
            @QueryParam("technology") String technology
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        var result = inventory.find(
                current.user().getId(),
                page,
                pageSize,
                search,
                ownership,
                visibility,
                activity,
                category,
                technology
        );

        List<Item> items = result.items().stream()
                .map(repository -> toItem(
                        current.user().getId(),
                        repository
                ))
                .toList();

        return new Response(
                items,
                result.total(),
                result.page(),
                result.pageSize(),
                (int) Math.ceil(
                        result.total() / (double) result.pageSize()
                )
        );
    }

    private Item toItem(UUID userId, SourceRepository repository) {
        var categoryItems = categories.findForRepository(repository.getId())
                .stream()
                .map(category -> new Category(
                        category.getCategory().getCategoryKey(),
                        category.getCategory().getDisplayName()
                ))
                .distinct()
                .toList();

        var technologyItems = technologies.findForRepository(
                        userId,
                        repository.getId()
                ).stream()
                .map(evidence -> new Technology(
                        evidence.getTechnology().getTechnologyKey(),
                        evidence.getTechnology().getDisplayName()
                ))
                .distinct()
                .toList();

        return new Item(
                repository.getId(),
                repository.getName(),
                repository.getDescription(),
                repository.getHtmlUrl(),
                repository.getOwnershipRelation().name(),
                repository.getVisibility().name(),
                repository.getLastActivityAt(),
                categoryItems,
                technologyItems
        );
    }

    public record Response(
            List<Item> items,
            long total,
            int page,
            int pageSize,
            int totalPages
    ) {}

    public record Item(
            UUID id,
            String name,
            String description,
            String htmlUrl,
            String ownershipRelation,
            String visibility,
            OffsetDateTime lastActivityAt,
            List<Category> categories,
            List<Technology> technologies
    ) {}

    public record Category(
            String key,
            String name
    ) {}

    public record Technology(
            String key,
            String name
    ) {}
}
