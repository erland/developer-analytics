package io.github.developeranalytics.domain.external;

import io.github.developeranalytics.domain.model.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "returned_ai_assessment")
public class ReturnedAiAssessment {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_client_token_id")
    private ExternalClientToken externalClientToken;

    @Column(name = "analysis_type", nullable = false, length = 120)
    private String analysisType;

    @Column(name = "source_client", nullable = false, length = 120)
    private String sourceClient;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_scope", nullable = false, length = 50)
    private ExternalClientToken.PrivacyScope dataScope;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> content = new LinkedHashMap<>();

    @Column(name = "contains_private_data", nullable = false)
    private boolean containsPrivateData;

    protected ReturnedAiAssessment() {}

    public ReturnedAiAssessment(
            AppUser user,
            ExternalClientToken externalClientToken,
            String analysisType,
            String sourceClient,
            ExternalClientToken.PrivacyScope dataScope,
            Map<String, Object> content,
            boolean containsPrivateData
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.externalClientToken = externalClientToken;
        this.analysisType = requireText(analysisType, "analysisType");
        this.sourceClient = requireText(sourceClient, "sourceClient");
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.dataScope = dataScope;
        this.content = new LinkedHashMap<>(content);
        this.containsPrivateData = containsPrivateData;
    }

    private String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public ExternalClientToken getExternalClientToken() { return externalClientToken; }
    public String getAnalysisType() { return analysisType; }
    public String getSourceClient() { return sourceClient; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public ExternalClientToken.PrivacyScope getDataScope() { return dataScope; }
    public Map<String, Object> getContent() { return Map.copyOf(content); }
    public boolean isContainsPrivateData() { return containsPrivateData; }
}
