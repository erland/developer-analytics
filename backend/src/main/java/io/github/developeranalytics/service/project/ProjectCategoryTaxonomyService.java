package io.github.developeranalytics.service.project;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.developeranalytics.domain.project.ProjectCategory;
import io.github.developeranalytics.persistence.project.ProjectCategoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ProjectCategoryTaxonomyService {

    @Inject
    ProjectCategoryRepository repository;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public int seedBuiltInTaxonomyIfEmpty() {
        try (InputStream in = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("project-category-taxonomy.json")) {
            if (in == null) {
                throw new IllegalStateException(
                        "project-category-taxonomy.json not found"
                );
            }

            JsonNode root = objectMapper.readTree(in);
            int created = 0;

            for (JsonNode node : root.path("categories")) {
                String key = node.path("key").asText();
                if (repository.findByKey(key).isPresent()) {
                    continue;
                }

                List<String> aliases = new ArrayList<>();
                for (JsonNode alias : node.path("aliases")) {
                    aliases.add(alias.asText());
                }

                repository.persist(new ProjectCategory(
                        key,
                        node.path("name").asText(),
                        node.path("description").asText(null),
                        aliases,
                        node.path("sortOrder").asInt(0)
                ));
                created++;
            }

            return created;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to seed project category taxonomy",
                    e
            );
        }
    }

    public List<ProjectCategory> activeTaxonomy() {
        return repository.findActive();
    }
}
