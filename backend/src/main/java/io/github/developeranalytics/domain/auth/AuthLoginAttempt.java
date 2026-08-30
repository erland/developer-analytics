package io.github.developeranalytics.domain.auth;

import io.github.developeranalytics.domain.model.AppUser;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="auth_login_attempt")
public class AuthLoginAttempt {
    public enum Purpose { LOGIN, PRIVATE_REPOSITORY_ACCESS }

    @Id
    @GeneratedValue(strategy=GenerationType.UUID)
    private UUID id;

    @Column(name="state_hash", nullable=false, unique=true, length=128)
    private String stateHash;

    @Column(name="pkce_verifier", nullable=false, length=255)
    private String pkceVerifier;

    @Column(name="expires_at", nullable=false)
    private OffsetDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name="purpose", nullable=false, length=40)
    private Purpose purpose = Purpose.LOGIN;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="user_id")
    private AppUser user;

    protected AuthLoginAttempt() {}

    public AuthLoginAttempt(
            String stateHash,
            String pkceVerifier,
            OffsetDateTime expiresAt
    ) {
        this(stateHash, pkceVerifier, expiresAt, Purpose.LOGIN, null);
    }

    public AuthLoginAttempt(
            String stateHash,
            String pkceVerifier,
            OffsetDateTime expiresAt,
            Purpose purpose,
            AppUser user
    ) {
        this.stateHash = stateHash;
        this.pkceVerifier = pkceVerifier;
        this.expiresAt = expiresAt;
        this.purpose = purpose;
        this.user = user;
    }

    public String getPkceVerifier() { return pkceVerifier; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public Purpose getPurpose() { return purpose; }
    public AppUser getUser() { return user; }
}
