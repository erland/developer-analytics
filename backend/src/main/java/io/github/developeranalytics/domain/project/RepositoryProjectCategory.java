package io.github.developeranalytics.domain.project;

import io.github.developeranalytics.domain.model.SourceRepository;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Table(name = "repository_project_category")
@IdClass(RepositoryProjectCategoryId.class)
public class RepositoryProjectCategory {

    public enum Source {
        DETERMINISTIC,
        AI,
        MANUAL
    }

    public enum Confidence {
        LOW,
        MEDIUM,
        HIGH
    }

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private SourceRepository repository;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProjectCategory category;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Source source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Confidence confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rationale = new LinkedHashMap<>();

    @Column(name = "observed_at", nullable = false)
    private OffsetDateTime observedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_provenance", nullable = false, length = 32)
    private io.github.developeranalytics.domain.model.DataPrivacyProvenance privacyProvenance = io.github.developeranalytics.domain.model.DataPrivacyProvenance.PUBLIC_ONLY;

    protected RepositoryProjectCategory() {}

    public RepositoryProjectCategory(
            SourceRepository repository,
            ProjectCategory category,
            Source source,
            Confidence confidence,
            Map<String, Object> rationale,
            OffsetDateTime observedAt
    ) {
        this.repository = repository;
        this.category = category;
        this.source = source;
        this.confidence = confidence;
        this.rationale = rationale == null
                ? new LinkedHashMap<>()
                : new LinkedHashMap<>(rationale);
        this.observedAt = observedAt;
        this.privacyProvenance = io.github.developeranalytics.domain.model.DataPrivacyProvenance.fromVisibility(repository.getVisibility());
    }

    public SourceRepository getRepository() { return repository; }
    public ProjectCategory getCategory() { return category; }
    public Source getSource() { return source; }
    public Confidence getConfidence() { return confidence; }
    public Map<String, Object> getRationale() { return Map.copyOf(rationale); }
    public OffsetDateTime getObservedAt() { return observedAt; }
    public io.github.developeranalytics.domain.model.DataPrivacyProvenance getPrivacyProvenance() { return privacyProvenance; }
}
