package io.github.developeranalytics.domain.technology;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "repository_technology_evidence")
public class RepositoryTechnologyEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private SourceRepository repository;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technology_id", nullable = false)
    private TechnologyCatalogueEntry technology;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 32)
    private TechnologyEvidenceType evidenceType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TechnologyEvidenceStrength strength;

    @Column(name = "source_value", length = 255)
    private String sourceValue;

    @Column(name = "measured_value")
    private Long measuredValue;

    @Column(name = "observed_at", nullable = false)
    private OffsetDateTime observedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;

    protected RepositoryTechnologyEvidence() {}

    public RepositoryTechnologyEvidence(
            AppUser user,
            SourceRepository repository,
            TechnologyCatalogueEntry technology,
            TechnologyEvidenceType evidenceType,
            TechnologyEvidenceStrength strength,
            String sourceValue,
            Long measuredValue,
            OffsetDateTime observedAt
    ) {
        this.user = user;
        this.repository = repository;
        this.technology = technology;
        this.evidenceType = evidenceType;
        this.strength = strength;
        this.sourceValue = sourceValue;
        this.measuredValue = measuredValue;
        this.observedAt = observedAt;
    }

    public void refresh(Long measuredValue, OffsetDateTime observedAt) {
        this.measuredValue = measuredValue;
        this.observedAt = observedAt;
        this.strength = TechnologyEvidenceStrength.OBSERVED;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public SourceRepository getRepository() { return repository; }
    public TechnologyCatalogueEntry getTechnology() { return technology; }
    public TechnologyEvidenceType getEvidenceType() { return evidenceType; }
    public TechnologyEvidenceStrength getStrength() { return strength; }
    public String getSourceValue() { return sourceValue; }
    public Long getMeasuredValue() { return measuredValue; }
    public OffsetDateTime getObservedAt() { return observedAt; }
}
