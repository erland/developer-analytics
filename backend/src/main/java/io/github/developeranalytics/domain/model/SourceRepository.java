package io.github.developeranalytics.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.ArrayList;
import java.util.List;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "source_repository")
public class SourceRepository {
    public static final int CURRENT_ANALYSIS_VERSION = 1;

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 50) private String provider;
    @Column(name = "external_repository_id", nullable = false, length = 255) private String externalRepositoryId;
    @Column(name = "owner_external_id", length = 255) private String ownerExternalId;
    @Column(name = "owner_login", length = 255) private String ownerLogin;
    @Enumerated(EnumType.STRING) @Column(name = "owner_type", nullable = false) private RepositoryOwnerType ownerType = RepositoryOwnerType.USER;
    @Enumerated(EnumType.STRING) @Column(name = "ownership_relation", nullable = false) private RepositoryOwnershipRelation ownershipRelation = RepositoryOwnershipRelation.OWNED_BY_USER;
    @Column(nullable = false, length = 255) private String name;
    @Column(name = "full_name", length = 512) private String fullName;
    @Column(name = "html_url") private String htmlUrl;
    @Column(columnDefinition = "text") private String description;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> topics = new ArrayList<>();
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RepositoryVisibility visibility = RepositoryVisibility.PUBLIC;
    @Column(name = "is_fork", nullable = false) private boolean fork;
    @Column(name = "is_archived", nullable = false) private boolean archived;
    @Column(name = "first_activity_at") private OffsetDateTime firstActivityAt;
    @Column(name = "last_activity_at") private OffsetDateTime lastActivityAt;
    @Enumerated(EnumType.STRING) @Column(name = "sync_status", nullable = false) private RepositorySyncStatus syncStatus = RepositorySyncStatus.NOT_SYNCED;
    @Column(name = "last_synced_at") private OffsetDateTime lastSyncedAt;
    @Column(name = "discovered_at") private OffsetDateTime discoveredAt;
    @Column(name = "last_seen_at") private OffsetDateTime lastSeenAt;
    @Column(name = "sync_error") private String syncError;
    @Column(name = "included_in_analysis", nullable = false) private boolean includedInAnalysis = true;
    @Column(name = "contributor_count") private Integer contributorCount;
    @Column(name = "human_contributor_count") private Integer humanContributorCount;
    @Column(name = "bot_contributor_count") private Integer botContributorCount;
    @Column(name = "user_commit_count") private Integer userCommitCount;
    @Column(name = "repository_commit_count") private Integer repositoryCommitCount;
    @Column(name = "user_additions") private Long userAdditions;
    @Column(name = "user_deletions") private Long userDeletions;
    @Column(name = "contributor_stats_at") private OffsetDateTime contributorStatsAt;
    @Column(name = "contribution_scope_version", nullable = false) private int contributionScopeVersion;
    @Column(name = "analysis_completed_at") private OffsetDateTime analysisCompletedAt;
    @Column(name = "analyzed_activity_at") private OffsetDateTime analyzedActivityAt;
    @Column(name = "analysis_version", nullable = false) private int analysisVersion;

    protected SourceRepository() {}

    public SourceRepository(AppUser user, String provider, String externalRepositoryId, String ownerLogin, String name) {
        this.user = user; this.provider = provider; this.externalRepositoryId = externalRepositoryId;
        this.ownerLogin = ownerLogin; this.name = name;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getProvider() { return provider; }
    public String getExternalRepositoryId() { return externalRepositoryId; }
    public String getName() { return name; }
    public RepositoryVisibility getVisibility() { return visibility; }
    public RepositoryOwnershipRelation getOwnershipRelation() { return ownershipRelation; }
    public RepositorySyncStatus getSyncStatus() { return syncStatus; }
    public OffsetDateTime getLastActivityAt() { return lastActivityAt; }
    public boolean isIncludedInAnalysis() { return includedInAnalysis; }
    public Integer getContributorCount() { return contributorCount; }
    public Integer getHumanContributorCount() { return humanContributorCount; }
    public Integer getBotContributorCount() { return botContributorCount; }
    public Integer getUserCommitCount() { return userCommitCount; }
    public Integer getRepositoryCommitCount() { return repositoryCommitCount; }
    public Long getUserAdditions() { return userAdditions; }
    public Long getUserDeletions() { return userDeletions; }
    public OffsetDateTime getContributorStatsAt() { return contributorStatsAt; }
    public int getContributionScopeVersion() { return contributionScopeVersion; }
    public OffsetDateTime getAnalysisCompletedAt() { return analysisCompletedAt; }
    public OffsetDateTime getAnalyzedActivityAt() { return analyzedActivityAt; }
    public int getAnalysisVersion() { return analysisVersion; }

    public boolean needsAnalysisRefresh() {
        if (!includedInAnalysis) return false;
        if (analysisCompletedAt == null || analysisVersion < CURRENT_ANALYSIS_VERSION) return true;
        if (lastActivityAt == null) return false;
        return analyzedActivityAt == null || lastActivityAt.isAfter(analyzedActivityAt);
    }

    public void markAnalysisCompleted(OffsetDateTime completedAt) {
        markAnalysisCompleted(completedAt, lastActivityAt);
    }

    public void markAnalysisCompleted(OffsetDateTime completedAt, OffsetDateTime activityWatermark) {
        analysisCompletedAt = completedAt;
        analyzedActivityAt = activityWatermark;
        analysisVersion = CURRENT_ANALYSIS_VERSION;
    }

    public void markContributionScopeCurrent() { contributionScopeVersion = 2; }
    public void updateContributorStatistics(int contributors, int humans, int bots, int commits, int repositoryCommits, long additions, long deletions, OffsetDateTime observedAt) {
        this.contributorCount = contributors;
        this.humanContributorCount = humans;
        this.botContributorCount = bots;
        this.userCommitCount = commits;
        this.repositoryCommitCount = repositoryCommits;
        this.userAdditions = additions;
        this.userDeletions = deletions;
        this.contributorStatsAt = observedAt;
    }

public String getOwnerExternalId() { return ownerExternalId; }
public String getOwnerLogin() { return ownerLogin; }
public String getFullName() { return fullName; }
public String getHtmlUrl() { return htmlUrl; }
public String getDescription() { return description; }
public List<String> getTopics() { return List.copyOf(topics); }
public boolean isFork() { return fork; }
public boolean isArchived() { return archived; }
public OffsetDateTime getDiscoveredAt() { return discoveredAt; }
public OffsetDateTime getLastSeenAt() { return lastSeenAt; }

    public void setVisibility(RepositoryVisibility v) { visibility = v; }
    public void setOwnershipRelation(RepositoryOwnershipRelation v) { ownershipRelation = v; }
    public void setSyncStatus(RepositorySyncStatus v) { syncStatus = v; }
    public void setLastActivityAt(OffsetDateTime v) { lastActivityAt = v; }
    public void setFork(boolean v) { fork = v; }
    public void setArchived(boolean v) { archived = v; }
    public void includeInAnalysis() { includedInAnalysis = true; }
    public void excludeFromAnalysis() { includedInAnalysis = false; }

    public void markSyncing() {
        syncStatus = RepositorySyncStatus.SYNCING;
        syncError = null;
    }

    public void markSynced(OffsetDateTime now) {
        syncStatus = RepositorySyncStatus.SYNCED;
        lastSyncedAt = now;
        syncError = null;
    }

    public void markSyncFailed(String error) {
        syncStatus = RepositorySyncStatus.FAILED;
        syncError = error;
    }

    public String getSyncError() { return syncError; }

    public void updateFromDiscovery(
            String ownerExternalId,
            String ownerLogin,
            String name,
            String fullName,
            String htmlUrl,
            String description,
            List<String> topics,
            RepositoryOwnerType ownerType,
            RepositoryOwnershipRelation ownershipRelation,
            RepositoryVisibility visibility,
            boolean fork,
            boolean archived,
            OffsetDateTime lastActivityAt,
            OffsetDateTime seenAt
    ) {
        this.ownerExternalId = ownerExternalId;
        this.ownerLogin = ownerLogin;
        this.name = name;
        this.fullName = fullName;
        this.htmlUrl = htmlUrl;
        this.description = description;
        this.topics = topics == null ? new ArrayList<>() : new ArrayList<>(topics);
        this.ownerType = ownerType;
        this.ownershipRelation = ownershipRelation;
        this.visibility = visibility;
        // GitHub App installation scope is the repository selection boundary.
        // Every repository returned by GitHub is therefore included in analysis;
        // Developer Analytics no longer maintains a second repository allow-list.
        this.includedInAnalysis = true;
        this.fork = fork;
        this.archived = archived;
        this.lastActivityAt = lastActivityAt;
        if (this.discoveredAt == null) {
            this.discoveredAt = seenAt;
        }
        this.lastSeenAt = seenAt;
    }
}
