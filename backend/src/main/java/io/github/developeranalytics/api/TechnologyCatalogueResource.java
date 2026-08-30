package io.github.developeranalytics.api;

import io.github.developeranalytics.domain.technology.TechnologyCatalogueEntry;
import io.github.developeranalytics.service.technology.TechnologyCatalogueService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/technology-catalogue")
@Produces(MediaType.APPLICATION_JSON)
public class TechnologyCatalogueResource {

    @Inject
    TechnologyCatalogueService catalogue;

    @GET
    public List<Entry> list() {
        catalogue.seedBuiltInCatalogueIfEmpty();
        return catalogue.activeCatalogue().stream()
                .map(Entry::from)
                .toList();
    }

    public record Entry(
            String key,
            String name,
            String category,
            List<String> aliases,
            List<String> languageEvidence,
            List<String> fileEvidence,
            List<String> manifestEvidence
    ) {
        static Entry from(TechnologyCatalogueEntry entry) {
            return new Entry(
                    entry.getTechnologyKey(),
                    entry.getDisplayName(),
                    entry.getCategory().name(),
                    entry.getAliases(),
                    entry.getLanguageEvidence(),
                    entry.getFileEvidence(),
                    entry.getManifestEvidence()
            );
        }
    }
}
