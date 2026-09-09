package io.github.developeranalytics.domain.model;

import jakarta.persistence.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "provider_sync_run")
public class ProviderSyncRun {

    public enum Status { RUNNING, COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 32)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_mode", nullable = false, length = 32)
    private ContributionSyncMode syncMode = ContributionSyncMode.UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status = Status.RUNNING;

    @Column(name = "repositories_planned", nullable = false)
    private int repositoriesPlanned;

    @Column(name = "rate_limit_pause_count", nullable = false)
    private int rateLimitPauseCount;

    @Column(name = "paused_duration_seconds", nullable = false)
    private long pausedDurationSeconds;

    @Column(name = "pause_started_at")
    private OffsetDateTime pauseStartedAt;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected ProviderSyncRun() {}

    public ProviderSyncRun(AppUser user, String provider, OffsetDateTime startedAt) {
        this.user = user;
        this.provider = provider;
        this.startedAt = startedAt;
    }

    public void setRepositoriesPlanned(int repositoriesPlanned) {
        this.repositoriesPlanned = Math.max(0, repositoriesPlanned);
    }

    public void observeSyncMode(ContributionSyncMode observed) {
        if (observed == null || observed == ContributionSyncMode.UNKNOWN) return;
        if (syncMode == ContributionSyncMode.UNKNOWN) {
            syncMode = observed;
        } else if (syncMode != observed) {
            syncMode = ContributionSyncMode.MIXED;
        }
    }

    public void pause(OffsetDateTime now) {
        if (now == null || pauseStartedAt != null || status != Status.RUNNING) return;
        pauseStartedAt = now;
        rateLimitPauseCount++;
    }

    public void resume(OffsetDateTime now) {
        if (now == null || pauseStartedAt == null) return;
        pausedDurationSeconds += Math.max(0, Duration.between(pauseStartedAt, now).getSeconds());
        pauseStartedAt = null;
    }

    public void complete(OffsetDateTime now) {
        resume(now);
        status = Status.COMPLETED;
        completedAt = now;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getProvider() { return provider; }
    public ContributionSyncMode getSyncMode() { return syncMode; }
    public Status getStatus() { return status; }
    public int getRepositoriesPlanned() { return repositoriesPlanned; }
    public int getRateLimitPauseCount() { return rateLimitPauseCount; }
    public long getPausedDurationSeconds() { return pausedDurationSeconds; }
    public OffsetDateTime getPauseStartedAt() { return pauseStartedAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
