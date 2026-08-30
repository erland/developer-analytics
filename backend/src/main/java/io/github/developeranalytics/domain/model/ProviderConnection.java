package io.github.developeranalytics.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "provider_connection")
public class ProviderConnection {
    public enum Status { CONNECTED, DISCONNECTED, ERROR }
    public enum PrivateRepositoryAccess { NOT_AUTHORISED, AUTHORISED }

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "provider_identity_id")
    private ProviderIdentity providerIdentity;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "connected_at", nullable = false)
    private OffsetDateTime connectedAt;

    @Column(name = "last_validated_at")
    private OffsetDateTime lastValidatedAt;

    @Column(name = "disconnected_at")
    private OffsetDateTime disconnectedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "credential_ciphertext")
    private String credentialCiphertext;

    @Column(name = "credential_key_version", length = 32)
    private String credentialKeyVersion;

    @Column(name = "credential_updated_at")
    private OffsetDateTime credentialUpdatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "private_repository_access", nullable = false, length = 30)
    private PrivateRepositoryAccess privateRepositoryAccess =
            PrivateRepositoryAccess.NOT_AUTHORISED;

    @Column(name = "private_repository_authorised_at")
    private OffsetDateTime privateRepositoryAuthorisedAt;

    protected ProviderConnection() {}

    public ProviderConnection(AppUser user, ProviderIdentity identity, String provider) {
        this.user = Objects.requireNonNull(user);
        this.providerIdentity = identity;
        this.provider = provider.trim().toLowerCase(Locale.ROOT);
        this.status = Status.CONNECTED.name();
    }

    public void markValidated() {
        this.status = Status.CONNECTED.name();
        this.lastValidatedAt = OffsetDateTime.now(ZoneOffset.UTC);
        this.disconnectedAt = null;
    }

    public void disconnect() {
        this.status = Status.DISCONNECTED.name();
        this.disconnectedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void setEncryptedCredential(String ciphertext, String keyVersion) {
        this.credentialCiphertext = ciphertext;
        this.credentialKeyVersion = keyVersion;
        this.credentialUpdatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void clearCredential() {
        this.credentialCiphertext = null;
        this.credentialKeyVersion = null;
        this.credentialUpdatedAt = null;
    }

    public void authorisePrivateRepositoryAccess() {
        this.privateRepositoryAccess = PrivateRepositoryAccess.AUTHORISED;
        this.privateRepositoryAuthorisedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public void removePrivateRepositoryAccess() {
        this.privateRepositoryAccess = PrivateRepositoryAccess.NOT_AUTHORISED;
        this.privateRepositoryAuthorisedAt = null;
    }

    public boolean isPrivateRepositoryAccessAuthorised() {
        return this.privateRepositoryAccess ==
                PrivateRepositoryAccess.AUTHORISED;
    }

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        createdAt = now; updatedAt = now;
        if (connectedAt == null) connectedAt = now;
    }
    @PreUpdate
    void onUpdate() { updatedAt = OffsetDateTime.now(ZoneOffset.UTC); }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public ProviderIdentity getProviderIdentity() { return providerIdentity; }
    public String getProvider() { return provider; }
    public String getStatus() { return status; }
    public OffsetDateTime getConnectedAt() { return connectedAt; }
    public OffsetDateTime getLastValidatedAt() { return lastValidatedAt; }
    public OffsetDateTime getDisconnectedAt() { return disconnectedAt; }
    public String getCredentialCiphertext() { return credentialCiphertext; }
    public String getCredentialKeyVersion() { return credentialKeyVersion; }
    public OffsetDateTime getCredentialUpdatedAt() { return credentialUpdatedAt; }
    public PrivateRepositoryAccess getPrivateRepositoryAccess() { return privateRepositoryAccess; }
    public OffsetDateTime getPrivateRepositoryAuthorisedAt() { return privateRepositoryAuthorisedAt; }
}
