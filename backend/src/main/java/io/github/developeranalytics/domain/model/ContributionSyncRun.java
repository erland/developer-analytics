package io.github.developeranalytics.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "contribution_sync_run")
public class ContributionSyncRun {
    public enum Status { QUEUED, RUNNING, COMPLETED, FAILED, RATE_LIMITED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "repository_id", nullable = false)
    private SourceRepository repository;

    @Column(nullable = false, length = 32)
    private String provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_mode", nullable = false, length = 32)
    private ContributionSyncMode syncMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "contributions_seen", nullable = false)
    private int contributionsSeen;

    @Column(name = "contributions_created", nullable = false)
    private int contributionsCreated;

    @Column(name = "contributions_updated", nullable = false)
    private int contributionsUpdated;

    @Column(name = "pages_processed", nullable = false)
    private int pagesProcessed;

    @Column(name = "api_request_count", nullable = false)
    private int apiRequestCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "api_requests_by_endpoint", columnDefinition = "jsonb", nullable = false)
    private Map<String, Integer> apiRequestsByEndpoint = Map.of();

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

    protected ContributionSyncRun() {}

    public ContributionSyncRun(AppUser user, SourceRepository repository, String provider) {
        this(user, repository, provider, ContributionSyncMode.UNKNOWN);
    }

    public ContributionSyncRun(
            AppUser user,
            SourceRepository repository,
            String provider,
            ContributionSyncMode syncMode
    ) {
        this.user = user;
        this.repository = repository;
        this.provider = provider;
        this.syncMode = syncMode == null ? ContributionSyncMode.UNKNOWN : syncMode;
        this.status = Status.QUEUED;
    }

    public void start(OffsetDateTime now) {
        status = Status.RUNNING;
        startedAt = now;
    }

    public void progress(int seen, int created, int updated, int pages,
                         Integer remaining, OffsetDateTime resetAt) {
        contributionsSeen = seen;
        contributionsCreated = created;
        contributionsUpdated = updated;
        pagesProcessed = pages;
        rateLimitRemaining = remaining;
        rateLimitResetAt = resetAt;
    }

    public void apiUsage(int requestCount, Map<String, Integer> requestsByEndpoint) {
        apiRequestCount = Math.max(0, requestCount);
        apiRequestsByEndpoint = Map.copyOf(requestsByEndpoint == null ? Map.of() : requestsByEndpoint);
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

    public void pauseForRateLimit(OffsetDateTime resetAt, OffsetDateTime now) {
        status = Status.RATE_LIMITED;
        lastError = null;
        rateLimitResetAt = resetAt;
        completedAt = now;
    }

    public UUID getId() { return id; }
    public AppUser getUser() { return user; }
    public SourceRepository getRepository() { return repository; }
    public String getProvider() { return provider; }
    public ContributionSyncMode getSyncMode() { return syncMode; }
    public Status getStatus() { return status; }
    public int getContributionsSeen() { return contributionsSeen; }
    public int getContributionsCreated() { return contributionsCreated; }
    public int getContributionsUpdated() { return contributionsUpdated; }
    public int getPagesProcessed() { return pagesProcessed; }
    public int getApiRequestCount() { return apiRequestCount; }
    public Map<String, Integer> getApiRequestsByEndpoint() { return apiRequestsByEndpoint; }
    public Integer getRateLimitRemaining() { return rateLimitRemaining; }
    public OffsetDateTime getRateLimitResetAt() { return rateLimitResetAt; }
    public OffsetDateTime getStartedAt() { return startedAt; }
    public OffsetDateTime getCompletedAt() { return completedAt; }
    public String getLastError() { return lastError; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
