package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.observability.StructuredLog;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import io.github.developeranalytics.provider.ProviderException;
import io.github.developeranalytics.provider.github.GitHubRateLimitService;
import io.github.developeranalytics.service.job.JobFailureClassifier;
import io.github.developeranalytics.service.sync.ContributionScopeUpgradeService;
import io.github.developeranalytics.service.sync.SynchronisationRecoveryService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@ApplicationScoped
public class BackgroundJobWorker {
    private static final Logger LOG = Logger.getLogger(BackgroundJobWorker.class);
    @Inject BackgroundJobRepository jobs;
    @Inject BackgroundJobDispatcher dispatcher;
    @Inject JobFailureClassifier failureClassifier;
    @Inject SynchronisationRecoveryService recovery;
    @Inject ContributionScopeUpgradeService contributionScopeUpgrades;
    @Inject GitHubRateLimitJobGate githubRateLimitGate;
    @ConfigProperty(name="developer-analytics.runtime-role", defaultValue="api") String runtimeRole;
    @ConfigProperty(name="developer-analytics.worker.id", defaultValue="worker-1") String workerId;

    @Scheduled(every="{developer-analytics.worker.poll-interval}", concurrentExecution=Scheduled.ConcurrentExecution.SKIP)
    @Transactional
    void poll() {
        if(!"worker".equalsIgnoreCase(runtimeRole)) return;
        jobs.claimNext(workerId, OffsetDateTime.now()).ifPresent(this::execute);
    }

    @Scheduled(every="60s", concurrentExecution=Scheduled.ConcurrentExecution.SKIP)
    void recoverInterruptedJobs() {
        if(!"worker".equalsIgnoreCase(runtimeRole)) return;
        recovery.recoverInterruptedJobs();
    }

    @Scheduled(every="60s", delayed="10s", concurrentExecution=Scheduled.ConcurrentExecution.SKIP)
    void enqueueContributionScopeUpgrades() {
        if(!"worker".equalsIgnoreCase(runtimeRole)) return;
        contributionScopeUpgrades.enqueueMissingBackfills();
    }

    void execute(BackgroundJob job) {
        MDC.put("backgroundJobId", job.getId().toString());
        try {
            Optional<GitHubRateLimitService.GitHubRateLimitDecision> blocked =
                    githubRateLimitGate.blockingDecision(job);
            if (blocked.isPresent()) {
                GitHubRateLimitService.GitHubRateLimitDecision decision = blocked.get();
                job.deferWithoutAttempt(decision.resumeAt());
                StructuredLog.info(
                        LOG,
                        "background_job_deferred_github_rate_limit",
                        StructuredLog.fields(
                                "backgroundJobId", job.getId(),
                                "jobType", job.getJobType(),
                                "resumeAt", decision.resumeAt(),
                                "remaining", decision.remaining(),
                                "reserve", decision.reserve(),
                                "secondaryLimited", decision.secondaryLimited()
                        )
                );
                return;
            }

            StructuredLog.info(
                    LOG,
                    "background_job_started",
                    StructuredLog.fields(
                            "backgroundJobId", job.getId(),
                            "jobType", job.getJobType(),
                            "attempt", job.getAttemptCount()
                    )
            );

            dispatcher.dispatch(job);
            job.complete();

            StructuredLog.info(
                    LOG,
                    "background_job_completed",
                    StructuredLog.fields(
                            "backgroundJobId", job.getId(),
                            "jobType", job.getJobType()
                    )
            );
        } catch(Exception e) {
            JobFailureClassifier.Classification classification = failureClassifier.classify(e);

            StructuredLog.warn(
                    LOG,
                    "background_job_failed",
                    e,
                    StructuredLog.fields(
                            "backgroundJobId", job.getId(),
                            "jobType", job.getJobType(),
                            "attempt", job.getAttemptCount(),
                            "retriable", classification.retriable(),
                            "providerAccessLost", classification.providerAccessLost()
                    )
            );

            if (classification.providerAccessLost()) {
                recovery.markProviderAccessLost(job, e);
                job.failPermanently(classification.reason() + ": " + safeMessage(e));
                return;
            }

            if (!classification.retriable()) {
                job.failPermanently(classification.reason() + ": " + safeMessage(e));
                return;
            }

            long backoffSeconds = Math.min(
                    900,
                    5L * (1L << Math.min(7, Math.max(0, job.getAttemptCount() - 1)))
            );
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            OffsetDateTime nextExecution = now.plusSeconds(backoffSeconds);
            OffsetDateTime providerRetryAt = providerRetryAt(e);
            if (providerRetryAt != null && providerRetryAt.isAfter(nextExecution)) {
                nextExecution = providerRetryAt;
            }

            job.retryOrFail(
                    classification.reason() + ": " + safeMessage(e),
                    nextExecution
            );
        } finally {
            MDC.remove("backgroundJobId");
        }
    }

    private OffsetDateTime providerRetryAt(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ProviderException providerException && providerException.getRetryAt() != null) {
                return providerException.getRetryAt();
            }
            current = current.getCause();
        }
        return null;
    }

    private String safeMessage(Throwable failure) {
        return failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage();
    }
}
