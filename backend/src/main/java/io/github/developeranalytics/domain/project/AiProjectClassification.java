package io.github.developeranalytics.domain.project;

import io.github.developeranalytics.domain.model.DataPrivacyProvenance;
import io.github.developeranalytics.domain.model.SourceRepository;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ai_project_classification")
public class AiProjectClassification {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private SourceRepository repository;

    @Column(name = "input_fingerprint", nullable = false, length = 64)
    private String inputFingerprint;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "classifications", nullable = false, columnDefinition = "jsonb")
    private List<String> classifications = new ArrayList<>();

    @Column(nullable = false)
    private double confidence;

    @Column(nullable = false, columnDefinition = "text")
    private String explanation;

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

    protected AiProjectClassification() {}

    public AiProjectClassification(
            SourceRepository repository,
            String inputFingerprint,
            List<String> classifications,
            double confidence,
            String explanation,
            String analysisVersion,
            String providerId,
            String modelId,
            DataPrivacyProvenance privacyProvenance,
            OffsetDateTime createdAt
    ) {
        this.id = UUID.randomUUID();
        this.repository = repository;
        this.inputFingerprint = inputFingerprint;
        this.classifications = List.copyOf(classifications);
        this.confidence = confidence;
        this.explanation = explanation;
        this.analysisVersion = analysisVersion;
        this.providerId = providerId;
        this.modelId = modelId;
        this.privacyProvenance = privacyProvenance;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public SourceRepository getRepository() { return repository; }
    public String getInputFingerprint() { return inputFingerprint; }
    public List<String> getClassifications() { return List.copyOf(classifications); }
    public double getConfidence() { return confidence; }
    public String getExplanation() { return explanation; }
    public String getAnalysisVersion() { return analysisVersion; }
    public String getProviderId() { return providerId; }
    public String getModelId() { return modelId; }
    public DataPrivacyProvenance getPrivacyProvenance() { return privacyProvenance; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
