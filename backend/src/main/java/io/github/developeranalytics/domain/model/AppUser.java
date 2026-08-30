package io.github.developeranalytics.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class AppUser {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_privacy_policy", nullable = false, length = 40)
    private io.github.developeranalytics.ai.AiPrivacyPolicy aiPrivacyPolicy =
            io.github.developeranalytics.ai.AiPrivacyPolicy.PRIVATE_AI_DISABLED;

    protected AppUser() {}

    public static AppUser create() { return new AppUser(); }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(ZoneOffset.UTC); }

    public UUID getId() { return id; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public io.github.developeranalytics.ai.AiPrivacyPolicy getAiPrivacyPolicy() {
        return aiPrivacyPolicy;
    }
    public void setAiPrivacyPolicy(
            io.github.developeranalytics.ai.AiPrivacyPolicy aiPrivacyPolicy
    ) {
        if (aiPrivacyPolicy == null) {
            throw new IllegalArgumentException("AI privacy policy is required");
        }
        this.aiPrivacyPolicy = aiPrivacyPolicy;
    }
}
