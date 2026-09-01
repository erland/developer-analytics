package io.github.developeranalytics.domain.job;

import io.github.developeranalytics.domain.model.AppUser;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Entity @Table(name="background_job")
public class BackgroundJob {
    @Id @GeneratedValue(strategy=GenerationType.UUID) private UUID id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private AppUser user;
    @Column(name="job_type", nullable=false) private String jobType;
    @Enumerated(EnumType.STRING) @Column(nullable=false) private BackgroundJobStatus status = BackgroundJobStatus.QUEUED;
    @Column(nullable=false) private int priority=100;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private Map<String,Object> payload;
    @Column(name="progress_percent") private Integer progressPercent;
    @Column(name="attempt_count", nullable=false) private int attemptCount;
    @Column(name="max_attempts", nullable=false) private int maxAttempts=5;
    @Column(name="next_execution_at", nullable=false) private OffsetDateTime nextExecutionAt;
    @Column(name="locked_at") private OffsetDateTime lockedAt;
    @Column(name="locked_by") private String lockedBy;
    @Column(name="last_error") private String lastError;
    @Column(name="completed_at") private OffsetDateTime completedAt;
    @Column(name="created_at", insertable=false, updatable=false) private OffsetDateTime createdAt;
    @Column(name="deduplication_key", length=255) private String deduplicationKey;
    protected BackgroundJob() {}

    public static BackgroundJob queuedDeduplicated(
            AppUser user,
            String jobType,
            int priority,
            Map<String,Object> payload,
            int maxAttempts,
            OffsetDateTime nextExecutionAt,
            String deduplicationKey
    ) {
        BackgroundJob job = queued(
                user, jobType, priority, payload, maxAttempts, nextExecutionAt);
        job.deduplicationKey = deduplicationKey;
        return job;
    }

    public static BackgroundJob queued(
            AppUser user,
            String jobType,
            int priority,
            Map<String,Object> payload,
            int maxAttempts,
            OffsetDateTime nextExecutionAt
    ) {
        BackgroundJob job = new BackgroundJob();
        job.user = user;
        job.jobType = jobType;
        job.priority = priority;
        job.payload = payload;
        job.maxAttempts = maxAttempts;
        job.nextExecutionAt = nextExecutionAt;
        job.status = BackgroundJobStatus.QUEUED;
        return job;
    }
    public UUID getId(){return id;} public AppUser getUser(){return user;} public String getJobType(){return jobType;} public BackgroundJobStatus getStatus(){return status;} public Map<String,Object> getPayload(){return payload;} public String getDeduplicationKey(){return deduplicationKey;}
    public int getAttemptCount(){return attemptCount;} public int getMaxAttempts(){return maxAttempts;}
    public Integer getProgressPercent(){return progressPercent;} public String getLastError(){return lastError;} public OffsetDateTime getNextExecutionAt(){return nextExecutionAt;} public OffsetDateTime getCompletedAt(){return completedAt;} public OffsetDateTime getCreatedAt(){return createdAt;}
    public void markRunning(String worker, OffsetDateTime now){status=BackgroundJobStatus.RUNNING; lockedBy=worker; lockedAt=now; attemptCount++;}
    public void complete(){status=BackgroundJobStatus.COMPLETED; progressPercent=100; completedAt=OffsetDateTime.now(); lockedAt=null; lockedBy=null;}
    public void retryOrFail(String error, OffsetDateTime next){
        lastError=error;
        lockedAt=null;
        lockedBy=null;
        if(attemptCount>=maxAttempts) {
            status=BackgroundJobStatus.FAILED;
        } else {
            status=BackgroundJobStatus.WAITING;
            nextExecutionAt=next;
        }
    }

    public void failPermanently(String error){
        lastError=error;
        lockedAt=null;
        lockedBy=null;
        status=BackgroundJobStatus.FAILED;
        completedAt=OffsetDateTime.now();
    }

    public void recoverInterrupted(OffsetDateTime nextExecution){
        if(status!=BackgroundJobStatus.RUNNING) return;
        status=BackgroundJobStatus.WAITING;
        lockedAt=null;
        lockedBy=null;
        lastError="Recovered after interrupted worker execution";
        nextExecutionAt=nextExecution;
    }

    public OffsetDateTime getLockedAt(){return lockedAt;}
}
