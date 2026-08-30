package io.github.developeranalytics.domain.technology;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "technology_catalogue")
public class TechnologyCatalogueEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "technology_key", nullable = false, unique = true, length = 100)
    private String technologyKey;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TechnologyCategory category;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "homepage_url", columnDefinition = "text")
    private String homepageUrl;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> aliases = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "language_evidence", nullable = false, columnDefinition = "jsonb")
    private List<String> languageEvidence = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "file_evidence", nullable = false, columnDefinition = "jsonb")
    private List<String> fileEvidence = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "manifest_evidence", nullable = false, columnDefinition = "jsonb")
    private List<String> manifestEvidence = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    protected TechnologyCatalogueEntry() {}

    public TechnologyCatalogueEntry(
            String technologyKey,
            String displayName,
            TechnologyCategory category,
            String description,
            String homepageUrl,
            List<String> aliases,
            List<String> languageEvidence,
            List<String> fileEvidence,
            List<String> manifestEvidence
    ) {
        this.technologyKey = requireKey(technologyKey);
        this.displayName = requireText(displayName, "displayName");
        this.category = category == null ? TechnologyCategory.OTHER : category;
        this.description = description;
        this.homepageUrl = homepageUrl;
        this.aliases = copy(aliases);
        this.languageEvidence = copy(languageEvidence);
        this.fileEvidence = copy(fileEvidence);
        this.manifestEvidence = copy(manifestEvidence);
    }

    private String requireKey(String value) {
        String normalized = requireText(value, "technologyKey")
                .trim()
                .toLowerCase()
                .replace(' ', '-');
        if (!normalized.matches("[a-z0-9][a-z0-9._-]*")) {
            throw new IllegalArgumentException("Invalid technologyKey: " + value);
        }
        return normalized;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private List<String> copy(List<String> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    public UUID getId() { return id; }
    public String getTechnologyKey() { return technologyKey; }
    public String getDisplayName() { return displayName; }
    public TechnologyCategory getCategory() { return category; }
    public String getDescription() { return description; }
    public String getHomepageUrl() { return homepageUrl; }
    public List<String> getAliases() { return List.copyOf(aliases); }
    public List<String> getLanguageEvidence() { return List.copyOf(languageEvidence); }
    public List<String> getFileEvidence() { return List.copyOf(fileEvidence); }
    public List<String> getManifestEvidence() { return List.copyOf(manifestEvidence); }
    public boolean isActive() { return active; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
