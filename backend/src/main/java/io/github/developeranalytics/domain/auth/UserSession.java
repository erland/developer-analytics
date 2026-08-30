package io.github.developeranalytics.domain.auth;

import io.github.developeranalytics.domain.model.AppUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="user_session")
public class UserSession {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private AppUser user;
    @Column(name="token_hash", nullable=false, unique=true, length=128) private String tokenHash;
    @Column(name="expires_at", nullable=false) private OffsetDateTime expiresAt;
    @Column(name="last_seen_at", nullable=false) private OffsetDateTime lastSeenAt;
    protected UserSession() {}
    public UserSession(AppUser user,String tokenHash,OffsetDateTime expiresAt){this.user=user;this.tokenHash=tokenHash;this.expiresAt=expiresAt;this.lastSeenAt=OffsetDateTime.now();}
    public AppUser getUser(){return user;} public OffsetDateTime getExpiresAt(){return expiresAt;}
    public void touch(){lastSeenAt=OffsetDateTime.now();}
}
