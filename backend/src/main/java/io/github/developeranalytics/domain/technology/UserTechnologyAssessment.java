package io.github.developeranalytics.domain.technology;

import io.github.developeranalytics.domain.model.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "user_technology_assessment")
public class UserTechnologyAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "technology_id", nullable = false)
    private TechnologyCatalogueEntry technology;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TechnologyEvidenceStrength strength;

    @Column(name = "repository_count", nullable = false)
    private int repositoryCount;

    @Column(name = "evidence_count", nullable = false)
    private int evidenceCount;

    @Column(name = "independent_evidence_types", nullable = false)
    private int independentEvidenceTypes;

    @Column(name = "first_observed_at")
    private OffsetDateTime firstObservedAt;

    @Column(name = "last_observed_at")
    private OffsetDateTime lastObservedAt;

    @Column(name = "recent_repository_count", nullable = false)
    private int recentRepositoryCount;

    @Column(nullable = false)
    private int score;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rationale = new LinkedHashMap<>();

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    protected UserTechnologyAssessment() {}

    public UserTechnologyAssessment(
            AppUser user,
            TechnologyCatalogueEntry technology
    ) {
        this.user = user;
        this.technology = technology;
    }

    public void update(
            TechnologyEvidenceStrength strength,
            int repositoryCount,
            int evidenceCount,
            int independentEvidenceTypes,
            OffsetDateTime firstObservedAt,
            OffsetDateTime lastObservedAt,
            int recentRepositoryCount,
            int score,
            Map<String, Object> rationale,
            OffsetDateTime calculatedAt
    ) {
        this.strength = strength;
        this.repositoryCount = repositoryCount;
        this.evidenceCount = evidenceCount;
        this.independentEvidenceTypes = independentEvidenceTypes;
        this.firstObservedAt = firstObservedAt;
        this.lastObservedAt = lastObservedAt;
        this.recentRepositoryCount = recentRepositoryCount;
        this.score = score;
        this.rationale = new LinkedHashMap<>(rationale);
        this.calculatedAt = calculatedAt;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public TechnologyCatalogueEntry getTechnology() { return technology; }
    public TechnologyEvidenceStrength getStrength() { return strength; }
    public int getRepositoryCount() { return repositoryCount; }
    public int getEvidenceCount() { return evidenceCount; }
    public int getIndependentEvidenceTypes() { return independentEvidenceTypes; }
    public OffsetDateTime getFirstObservedAt() { return firstObservedAt; }
    public OffsetDateTime getLastObservedAt() { return lastObservedAt; }
    public int getRecentRepositoryCount() { return recentRepositoryCount; }
    public int getScore() { return score; }
    public Map<String, Object> getRationale() { return Map.copyOf(rationale); }
    public OffsetDateTime getCalculatedAt() { return calculatedAt; }
}
