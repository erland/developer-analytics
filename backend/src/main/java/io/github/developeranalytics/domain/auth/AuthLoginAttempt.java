package io.github.developeranalytics.domain.auth;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="auth_login_attempt")
public class AuthLoginAttempt {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @Column(name="state_hash", nullable=false, unique=true, length=128) private String stateHash;
    @Column(name="pkce_verifier", nullable=false, length=255) private String pkceVerifier;
    @Column(name="expires_at", nullable=false) private OffsetDateTime expiresAt;
    protected AuthLoginAttempt() {}
    public AuthLoginAttempt(String stateHash, String pkceVerifier, OffsetDateTime expiresAt){this.stateHash=stateHash;this.pkceVerifier=pkceVerifier;this.expiresAt=expiresAt;}
    public String getPkceVerifier(){return pkceVerifier;} public OffsetDateTime getExpiresAt(){return expiresAt;}
}
