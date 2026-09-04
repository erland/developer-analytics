package io.github.developeranalytics.service.technology;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.domain.technology.TechnologyCatalogueEntry;
import io.github.developeranalytics.domain.technology.TechnologyCategory;
import io.github.developeranalytics.persistence.technology.TechnologyCatalogueRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class TechnologyCatalogueService {
    private static final Map<String, String> LANGUAGE_KEYS = Map.of(
            "c++", "cpp",
            "c#", "csharp",
            "f#", "fsharp",
            "objective-c", "objective-c",
            "objective-c++", "objective-cpp"
    );

    @Inject TechnologyCatalogueRepository repository;
    @Inject ObjectMapper objectMapper;

    @Transactional
    public int seedBuiltInCatalogueIfEmpty() {
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("technology-catalogue.json")) {
            if (in == null) throw new IllegalStateException("technology-catalogue.json not found");
            JsonNode root = objectMapper.readTree(in);
            int created = 0;
            for (JsonNode node : root.path("technologies")) {
                String key = node.path("key").asText();
                if (repository.findByKey(key).isPresent()) continue;
                TechnologyCatalogueEntry entry = new TechnologyCatalogueEntry(
                        key,
                        node.path("name").asText(),
                        TechnologyCategory.valueOf(node.path("category").asText()),
                        node.path("description").isMissingNode() ? null : node.path("description").asText(null),
                        node.path("homepageUrl").isMissingNode() ? null : node.path("homepageUrl").asText(null),
                        strings(node.path("aliases")), strings(node.path("languages")),
                        strings(node.path("files")), strings(node.path("manifests")));
                repository.persist(entry);
                created++;
            }
            return created;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to seed technology catalogue", e);
        }
    }

    @Transactional
    public TechnologyCatalogueEntry ensureLanguageTechnology(String providerLanguage) {
        String key = languageTechnologyKey(providerLanguage);
        return repository.findByKey(key).orElseGet(() -> {
            TechnologyCatalogueEntry entry = new TechnologyCatalogueEntry(
                    key,
                    providerLanguage,
                    TechnologyCategory.LANGUAGE,
                    "Language reported by the source-control provider",
                    null,
                    List.of(),
                    List.of(providerLanguage),
                    List.of(),
                    List.of());
            repository.persist(entry);
            return entry;
        });
    }

    static String languageTechnologyKey(String providerLanguage) {
        if (providerLanguage == null || providerLanguage.isBlank()) {
            throw new IllegalArgumentException("providerLanguage must not be blank");
        }
        String normalized = providerLanguage.trim().toLowerCase(Locale.ROOT);
        String known = LANGUAGE_KEYS.get(normalized);
        if (known != null) return known;

        String ascii = Normalizer.normalize(normalized, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}+", "");
        String key = ascii.replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "")
                .replaceAll("-{2,}", "-");
        if (key.isBlank()) {
            throw new IllegalArgumentException("Unable to derive language technology key: " + providerLanguage);
        }
        return key;
    }

    public List<TechnologyCatalogueEntry> activeCatalogue() { return repository.findActive(); }

    private List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (array.isArray()) for (JsonNode item : array) values.add(item.asText());
        return values;
    }
}
