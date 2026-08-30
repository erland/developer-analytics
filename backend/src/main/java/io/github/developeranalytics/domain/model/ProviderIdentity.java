package io.github.developeranalytics.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "provider_identity")
public class ProviderIdentity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(name = "external_user_id", nullable = false, length = 255)
    private String externalUserId;

    @Column(length = 255)
    private String login;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected ProviderIdentity() {}

    public ProviderIdentity(AppUser user, String provider, String externalUserId, String login, String displayName) {
        this.user = Objects.requireNonNull(user);
        this.provider = provider.trim().toLowerCase(Locale.ROOT);
        this.externalUserId = Objects.requireNonNull(externalUserId);
        this.login = login;
        this.displayName = displayName;
    }

    public void updateProfile(String login, String displayName) {
        this.login = login;
        this.displayName = displayName;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now; updatedAt = now;
    }
    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(ZoneOffset.UTC); }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getProvider() { return provider; }
    public String getExternalUserId() { return externalUserId; }
    public String getLogin() { return login; }
    public String getDisplayName() { return displayName; }
}
