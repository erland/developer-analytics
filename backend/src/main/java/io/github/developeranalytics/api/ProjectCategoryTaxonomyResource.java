package io.github.developeranalytics.api;

import io.github.developeranalytics.domain.project.ProjectCategory;
import io.github.developeranalytics.service.project.ProjectCategoryTaxonomyService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/project-category-taxonomy")
@Produces(MediaType.APPLICATION_JSON)
public class ProjectCategoryTaxonomyResource {

    @Inject
    ProjectCategoryTaxonomyService taxonomy;

    @GET
    public List<Entry> list() {
        taxonomy.seedBuiltInTaxonomyIfEmpty();
        return taxonomy.activeTaxonomy().stream()
                .map(Entry::from)
                .toList();
    }

    public record Entry(
            String key,
            String name,
            String description,
            List<String> aliases
    ) {
        static Entry from(ProjectCategory category) {
            return new Entry(
                    category.getCategoryKey(),
                    category.getDisplayName(),
                    category.getDescription(),
                    category.getAliases()
            );
        }
    }
}
