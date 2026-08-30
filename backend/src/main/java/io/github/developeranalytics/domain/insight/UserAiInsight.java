package io.github.developeranalytics.domain.insight;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.DataPrivacyProvenance;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_ai_insight")
public class UserAiInsight {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "input_fingerprint", nullable = false, length = 64)
    private String inputFingerprint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "likely_roles", nullable = false, columnDefinition = "jsonb")
    private List<Role> likelyRoles = new ArrayList<>();

    @Column(name = "technical_focus", nullable = false, columnDefinition = "text")
    private String technicalFocus;

    @Column(name = "breadth_depth_observation", nullable = false, columnDefinition = "text")
    private String breadthDepthObservation;

    @Column(name = "technology_evolution_summary", nullable = false, columnDefinition = "text")
    private String technologyEvolutionSummary;

    @Column(name = "open_source_engagement_summary", nullable = false, columnDefinition = "text")
    private String openSourceEngagementSummary;

    @Column(name = "analysis_version", nullable = false, length = 40)
    private String analysisVersion;

    @Column(name = "provider_id", nullable = false, length = 80)
    private String providerId;

    @Column(name = "model_id", nullable = false, length = 160)
    private String modelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_provenance", nullable = false, length = 32)
    private DataPrivacyProvenance privacyProvenance;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected UserAiInsight() {}

    public UserAiInsight(
            AppUser user,
            String inputFingerprint,
            List<Role> likelyRoles,
            String technicalFocus,
            String breadthDepthObservation,
            String technologyEvolutionSummary,
            String openSourceEngagementSummary,
            String analysisVersion,
            String providerId,
            String modelId,
            DataPrivacyProvenance privacyProvenance,
            OffsetDateTime createdAt
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.inputFingerprint = inputFingerprint;
        this.likelyRoles = new ArrayList<>(likelyRoles);
        this.technicalFocus = technicalFocus;
        this.breadthDepthObservation = breadthDepthObservation;
        this.technologyEvolutionSummary = technologyEvolutionSummary;
        this.openSourceEngagementSummary = openSourceEngagementSummary;
        this.analysisVersion = analysisVersion;
        this.providerId = providerId;
        this.modelId = modelId;
        this.privacyProvenance = privacyProvenance;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public List<Role> getLikelyRoles() { return List.copyOf(likelyRoles); }
    public String getTechnicalFocus() { return technicalFocus; }
    public String getBreadthDepthObservation() { return breadthDepthObservation; }
    public String getTechnologyEvolutionSummary() { return technologyEvolutionSummary; }
    public String getOpenSourceEngagementSummary() { return openSourceEngagementSummary; }
    public String getAnalysisVersion() { return analysisVersion; }
    public String getProviderId() { return providerId; }
    public String getModelId() { return modelId; }
    public DataPrivacyProvenance getPrivacyProvenance() { return privacyProvenance; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public String getInputFingerprint() { return inputFingerprint; }

    public record Role(
            String role,
            double confidence,
            String rationale
    ) {}
}
