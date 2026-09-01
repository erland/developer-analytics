package io.github.developeranalytics.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "contribution")
public class Contribution {
    public enum Type { COMMIT, PULL_REQUEST, REVIEW, ISSUE, RELEASE, MAINTENANCE }
    public enum State { OPEN, CLOSED, MERGED, PUBLISHED, UNKNOWN }

    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id", nullable = false) private AppUser user;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "source_repository_id", nullable = false) private SourceRepository repository;
    @Column(nullable = false, length = 32) private String provider;
    @Column(name = "provider_contribution_id", nullable = false) private String providerContributionId;
    @Enumerated(EnumType.STRING) @Column(name = "contribution_type", nullable = false) private Type type;
    @Column(columnDefinition = "text") private String title;
    @Column(name = "occurred_at", nullable = false) private OffsetDateTime occurredAt;
    @Enumerated(EnumType.STRING) private State state = State.UNKNOWN;
    private Integer additions;
    private Integer deletions;
    @Column(name = "changed_files") private Integer changedFiles;
    private Boolean merged;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "provider_metadata", columnDefinition = "jsonb") private Map<String,Object> providerMetadata;

    protected Contribution() {}

    public void updateFromDiscovery(
            String title,
            OffsetDateTime occurredAt,
            State state,
            Integer additions,
            Integer deletions,
            Integer changedFiles,
            Boolean merged
    ) {
        this.title = title;
        this.occurredAt = occurredAt;
        this.state = state == null ? State.UNKNOWN : state;
        this.additions = additions;
        this.deletions = deletions;
        this.changedFiles = changedFiles;
        this.merged = merged;
    }
    public Contribution(AppUser user, SourceRepository repository, String provider, String providerContributionId, Type type, OffsetDateTime occurredAt) {
        this.user=user; this.repository=repository; this.provider=provider; this.providerContributionId=providerContributionId; this.type=type; this.occurredAt=occurredAt;
    }

    public Type getType() { return type; }
    public String getProviderContributionId() { return providerContributionId; }
    public OffsetDateTime getOccurredAt() { return occurredAt; }
    public String getTitle() { return title; }
}
