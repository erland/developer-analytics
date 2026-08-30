package io.github.developeranalytics.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "repository_sync_run")
public class RepositorySyncRun {

    public enum Status { QUEUED, RUNNING, COMPLETED, FAILED, RATE_LIMITED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(nullable = false, length = 32)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "repositories_seen", nullable = false)
    private int repositoriesSeen;

    @Column(name = "repositories_created", nullable = false)
    private int repositoriesCreated;

    @Column(name = "repositories_updated", nullable = false)
    private int repositoriesUpdated;

    @Column(name = "pages_processed", nullable = false)
    private int pagesProcessed;

    @Column(name = "rate_limit_remaining")
    private Integer rateLimitRemaining;

    @Column(name = "rate_limit_reset_at")
    private OffsetDateTime rateLimitResetAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected RepositorySyncRun() {}

    public RepositorySyncRun(AppUser user, String provider) {
        this.user = user;
        this.provider = provider;
        this.status = Status.QUEUED;
    }

    public void start(OffsetDateTime now) {
        status = Status.RUNNING;
        startedAt = now;
    }

    public void progress(int seen, int created, int updated, int pages,
                         Integer remaining, OffsetDateTime resetAt) {
        repositoriesSeen = seen;
        repositoriesCreated = created;
        repositoriesUpdated = updated;
        pagesProcessed = pages;
        rateLimitRemaining = remaining;
        rateLimitResetAt = resetAt;
    }

    public void complete(OffsetDateTime now) {
        status = Status.COMPLETED;
        completedAt = now;
    }

    public void fail(String error, OffsetDateTime now) {
        status = Status.FAILED;
        lastError = error;
        completedAt = now;
    }

    public void rateLimited(String error, OffsetDateTime resetAt, OffsetDateTime now) {
        status = Status.RATE_LIMITED;
        lastError = error;
        rateLimitResetAt = resetAt;
        completedAt = now;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public String getProvider() { return provider; }
    public Status getStatus() { return status; }
    public int getRepositoriesSeen() { return repositoriesSeen; }
    public int getRepositoriesCreated() { return repositoriesCreated; }
    public int getRepositoriesUpdated() { return repositoriesUpdated; }
    public int getPagesProcessed() { return pagesProcessed; }
    public Integer getRateLimitRemaining() { return rateLimitRemaining; }
    public OffsetDateTime getRateLimitResetAt() { return rateLimitResetAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public String getLastError() { return lastError; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
