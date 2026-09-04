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
import java.util.ArrayList;
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
            @QueryParam("category") List<String> legacyCategories,
            @QueryParam("projectType") List<String> projectTypes,
            @QueryParam("technology") List<String> selectedTechnologies,
            @QueryParam("from") String from,
            @QueryParam("to") String to,
            @QueryParam("year") Integer year,
            @QueryParam("month") String month,
            @QueryParam("week") String week
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        var period = AnalysisPeriod.resolve(from, to, year, month, week);
        List<String> selectedCategories = new ArrayList<>();
        if (legacyCategories != null) selectedCategories.addAll(legacyCategories);
        if (projectTypes != null) selectedCategories.addAll(projectTypes);

        var result = inventory.find(
                current.user().getId(),
                page,
                pageSize,
                search,
                ownership,
                visibility,
                activity,
                selectedCategories,
                selectedTechnologies,
                period.from(),
                period.to()
        );

        List<Item> items = result.items().stream()
                .map(repository -> toItem(
                        current.user().getId(),
                        repository
                ))
                .toList();

        var matchingRepositoryIds = result.matchingRepositoryIds();
        var facetCategories = categories.summarizeForRepositories(matchingRepositoryIds).stream()
                .map(row -> new FacetValue(row.key(), row.name(), row.count()))
                .toList();
        var facetTechnologies = technologies.summarizeForRepositories(current.user().getId(), matchingRepositoryIds).stream()
                .map(row -> new FacetValue(row.key(), row.name(), row.count()))
                .toList();
        long ownCount = 0;
        long externalCount = 0;
        for (var row : inventory.ownershipFacets(matchingRepositoryIds)) {
            if ("OWNED_BY_USER".equals(row.key())) ownCount += row.count();
            else externalCount += row.count();
        }
        var ownershipFacets = new ArrayList<FacetValue>();
        if (ownCount > 0) ownershipFacets.add(new FacetValue("own", "Own", ownCount));
        if (externalCount > 0) ownershipFacets.add(new FacetValue("external", "External", externalCount));

        return new Response(
                items,
                result.total(),
                result.page(),
                result.pageSize(),
                (int) Math.ceil(result.total() / (double) result.pageSize()),
                new Facets(facetTechnologies, facetCategories, ownershipFacets)
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
                repository.getCodeSizeBytes(),
                repository.getRepositorySizeBytes(),
                categoryItems,
                technologyItems
        );
    }

    public record Response(
            List<Item> items,
            long total,
            int page,
            int pageSize,
            int totalPages,
            Facets facets
    ) {}

    public record Facets(
            List<FacetValue> technologies,
            List<FacetValue> projectTypes,
            List<FacetValue> ownership
    ) {}

    public record FacetValue(String key, String name, long count) {}

    public record Item(
            UUID id,
            String name,
            String description,
            String htmlUrl,
            String ownershipRelation,
            String visibility,
            OffsetDateTime lastActivityAt,
            Long codeSizeBytes,
            Long repositorySizeBytes,
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
