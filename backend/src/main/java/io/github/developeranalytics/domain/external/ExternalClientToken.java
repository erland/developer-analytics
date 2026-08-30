package io.github.developeranalytics.domain.external;

import io.github.developeranalytics.domain.model.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "external_client_token")
public class ExternalClientToken {

    public enum Scope {
        PROFILE_READ,
        PROJECTS_READ,
        ACTIVITY_READ,
        TECHNOLOGIES_READ,
        PROJECT_TYPES_READ,
        CONTRIBUTIONS_READ,
        EVIDENCE_READ,
        AI_ASSESSMENTS_WRITE
    }

    public enum PrivacyScope {
        PUBLIC_ONLY,
        PUBLIC_PLUS_PRIVATE_AGGREGATES,
        FULL_AUTHORISED_ANALYSIS;

        public boolean allowsPrivateAggregates() {
            return this != PUBLIC_ONLY;
        }

        public boolean allowsPrivateProjectDetail() {
            return this == FULL_AUTHORISED_ANALYSIS;
        }
    }

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Set<Scope> scopes = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "privacy_scope", nullable = false, length = 50)
    private PrivacyScope privacyScope = PrivacyScope.PUBLIC_ONLY;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_used_at")
    private OffsetDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    protected ExternalClientToken() {}

    public ExternalClientToken(
            AppUser user,
            String name,
            String tokenHash,
            Set<Scope> scopes,
            PrivacyScope privacyScope
    ) {
        this.id = UUID.randomUUID();
        this.user = user;
        this.name = name.trim();
        this.tokenHash = tokenHash;
        this.scopes = new LinkedHashSet<>(scopes);
        this.privacyScope = privacyScope;
        this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getName() { return name; }
    public Set<Scope> getScopes() { return Set.copyOf(scopes); }
    public PrivacyScope getPrivacyScope() { return privacyScope; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getLastUsedAt() { return lastUsedAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public boolean isRevoked() { return revokedAt != null; }

    public boolean hasScope(Scope scope) {
        return scopes.contains(scope);
    }

    public void markUsed() {
        lastUsedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void revoke() {
        if (revokedAt == null) {
            revokedAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}
