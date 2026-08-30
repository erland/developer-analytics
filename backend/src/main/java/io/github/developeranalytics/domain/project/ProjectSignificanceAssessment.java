package io.github.developeranalytics.domain.project;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "project_significance_assessment")
public class ProjectSignificanceAssessment {

    public enum Level {
        LOW,
        MEDIUM,
        HIGH,
        VERY_HIGH
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private SourceRepository repository;

    @Enumerated(EnumType.STRING)
    @Column(name = "significance_level", nullable = false, length = 32)
    private Level significanceLevel;

    @Column(name = "significance_score", nullable = false)
    private int significanceScore;

    @Column(name = "popularity_score", nullable = false)
    private int popularityScore;

    @Column(name = "contributor_score", nullable = false)
    private int contributorScore;

    @Column(name = "longevity_score", nullable = false)
    private int longevityScore;

    @Column(name = "ecosystem_score", nullable = false)
    private int ecosystemScore;

    @Column(name = "activity_score", nullable = false)
    private int activityScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "significance_rationale", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> significanceRationale = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "involvement_level", nullable = false, length = 32)
    private Level involvementLevel;

    @Column(name = "involvement_score", nullable = false)
    private int involvementScore;

    @Column(name = "contribution_score", nullable = false)
    private int contributionScore;

    @Column(name = "involvement_duration_score", nullable = false)
    private int involvementDurationScore;

    @Column(name = "involvement_recency_score", nullable = false)
    private int involvementRecencyScore;

    @Column(name = "relative_contribution_score", nullable = false)
    private int relativeContributionScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "involvement_rationale", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> involvementRationale = new LinkedHashMap<>();

    @Column(name = "calculated_at", nullable = false)
    private OffsetDateTime calculatedAt;

    protected ProjectSignificanceAssessment() {}

    public ProjectSignificanceAssessment(
            AppUser user,
            SourceRepository repository
    ) {
        this.user = user;
        this.repository = repository;
    }

    public void update(
            Level significanceLevel,
            int significanceScore,
            int popularityScore,
            int contributorScore,
            int longevityScore,
            int ecosystemScore,
            int activityScore,
            Map<String, Object> significanceRationale,
            Level involvementLevel,
            int involvementScore,
            int contributionScore,
            int involvementDurationScore,
            int involvementRecencyScore,
            int relativeContributionScore,
            Map<String, Object> involvementRationale,
            OffsetDateTime calculatedAt
    ) {
        this.significanceLevel = significanceLevel;
        this.significanceScore = significanceScore;
        this.popularityScore = popularityScore;
        this.contributorScore = contributorScore;
        this.longevityScore = longevityScore;
        this.ecosystemScore = ecosystemScore;
        this.activityScore = activityScore;
        this.significanceRationale = new LinkedHashMap<>(significanceRationale);

        this.involvementLevel = involvementLevel;
        this.involvementScore = involvementScore;
        this.contributionScore = contributionScore;
        this.involvementDurationScore = involvementDurationScore;
        this.involvementRecencyScore = involvementRecencyScore;
        this.relativeContributionScore = relativeContributionScore;
        this.involvementRationale = new LinkedHashMap<>(involvementRationale);

        this.calculatedAt = calculatedAt;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public SourceRepository getRepository() { return repository; }
    public Level getSignificanceLevel() { return significanceLevel; }
    public int getSignificanceScore() { return significanceScore; }
    public int getPopularityScore() { return popularityScore; }
    public int getContributorScore() { return contributorScore; }
    public int getLongevityScore() { return longevityScore; }
    public int getEcosystemScore() { return ecosystemScore; }
    public int getActivityScore() { return activityScore; }
    public Map<String, Object> getSignificanceRationale() {
        return Map.copyOf(significanceRationale);
    }
    public Level getInvolvementLevel() { return involvementLevel; }
    public int getInvolvementScore() { return involvementScore; }
    public int getContributionScore() { return contributionScore; }
    public int getInvolvementDurationScore() { return involvementDurationScore; }
    public int getInvolvementRecencyScore() { return involvementRecencyScore; }
    public int getRelativeContributionScore() { return relativeContributionScore; }
    public Map<String, Object> getInvolvementRationale() {
        return Map.copyOf(involvementRationale);
    }
    public OffsetDateTime getCalculatedAt() { return calculatedAt; }
}
