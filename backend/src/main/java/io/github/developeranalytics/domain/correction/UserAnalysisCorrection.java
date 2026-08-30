package io.github.developeranalytics.domain.correction;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "user_analysis_correction")
public class UserAnalysisCorrection {

    public enum Type {
        PROJECT_CATEGORY_REJECTED,
        TECHNOLOGY_INFERENCE_SUPPRESSED,
        PROJECT_EXCLUDED_FROM_AI_PROFILE
    }

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id")
    private SourceRepository repository;

    @Enumerated(EnumType.STRING)
    @Column(name = "correction_type", nullable = false, length = 50)
    private Type type;

    @Column(name = "correction_key", length = 200)
    private String correctionKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected UserAnalysisCorrection() {}

    public UserAnalysisCorrection(
            AppUser user,
            SourceRepository repository,
            Type type,
            String correctionKey
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.repository = repository;
        this.type = type;
        this.correctionKey = normalize(correctionKey);
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    private String normalize(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim().toLowerCase();
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public SourceRepository getRepository() { return repository; }
    public Type getType() { return type; }
    public String getCorrectionKey() { return correctionKey; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
