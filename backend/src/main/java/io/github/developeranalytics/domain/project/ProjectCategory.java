package io.github.developeranalytics.domain.project;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "project_category")
public class ProjectCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "category_key", nullable = false, unique = true, length = 100)
    private String categoryKey;

    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    @Column(columnDefinition = "text")
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> aliases = new ArrayList<>();

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    protected ProjectCategory() {}

    public ProjectCategory(
            String categoryKey,
            String displayName,
            String description,
            List<String> aliases,
            int sortOrder
    ) {
        this.categoryKey = normalizeKey(categoryKey);
        this.displayName = requireText(displayName, "displayName");
        this.description = description;
        this.aliases = aliases == null ? new ArrayList<>() : new ArrayList<>(aliases);
        this.sortOrder = sortOrder;
    }

    private String normalizeKey(String value) {
        String normalized = requireText(value, "categoryKey")
                .toLowerCase()
                .replace(' ', '-');
        if (!normalized.matches("[a-z0-9][a-z0-9._-]*")) {
            throw new IllegalArgumentException("Invalid categoryKey: " + value);
        }
        return normalized;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    public UUID getId() { return id; }
    public String getCategoryKey() { return categoryKey; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public List<String> getAliases() { return List.copyOf(aliases); }
    public boolean isActive() { return active; }
    public int getSortOrder() { return sortOrder; }
}
