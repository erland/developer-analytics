package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import io.github.developeranalytics.service.job.JobFailureClassifier;
import io.github.developeranalytics.service.sync.SynchronisationRecoveryService;
import io.github.developeranalytics.observability.StructuredLog;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.OffsetDateTime;

@ApplicationScoped
public class BackgroundJobWorker {
    private static final Logger LOG =
            Logger.getLogger(BackgroundJobWorker.class);
    @Inject BackgroundJobRepository jobs;
    @Inject BackgroundJobDispatcher dispatcher;
@Inject JobFailureClassifier failureClassifier;
@Inject SynchronisationRecoveryService recovery;
    @ConfigProperty(name="developer-analytics.runtime-role", defaultValue="api") String runtimeRole;
    @ConfigProperty(name="developer-analytics.worker.id", defaultValue="worker-1") String workerId;

    @Scheduled(every="{developer-analytics.worker.poll-interval}", concurrentExecution=Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        if(!"worker".equalsIgnoreCase(runtimeRole)) return;
        jobs.claimNext(workerId, OffsetDateTime.now()).ifPresent(this::execute);
    }

    @Scheduled(every="60s", concurrentExecution=Scheduled.ConcurrentExecution.SKIP)
    void recoverInterruptedJobs() {
        if(!"worker".equalsIgnoreCase(runtimeRole)) return;
        recovery.recoverInterruptedJobs();
    }

    @Transactional
    void execute(BackgroundJob job) {
        MDC.put("backgroundJobId", job.getId().toString());
        try {
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
            JobFailureClassifier.Classification classification =
                    failureClassifier.classify(e);

            StructuredLog.warn(
                    LOG,
                    "background_job_failed",
                    e,
                    StructuredLog.fields(
                            "backgroundJobId", job.getId(),
                            "jobType", job.getJobType(),
                            "attempt", job.getAttemptCount(),
                            "retriable", classification.retriable(),
                            "providerAccessLost",
                                    classification.providerAccessLost()
                    )
            );

            if (classification.providerAccessLost()) {
                recovery.markProviderAccessLost(job, e);
                job.failPermanently(
                        classification.reason() + ": " + safeMessage(e)
                );
                return;
            }

            if (!classification.retriable()) {
                job.failPermanently(
                        classification.reason() + ": " + safeMessage(e)
                );
                return;
            }

            long backoffSeconds = Math.min(
                    900,
                    5L * (1L << Math.min(
                            7,
                            Math.max(0, job.getAttemptCount() - 1)
                    ))
            );

            job.retryOrFail(
                    classification.reason() + ": " + safeMessage(e),
                    OffsetDateTime.now().plusSeconds(backoffSeconds)
            );
        } finally {
            MDC.remove("backgroundJobId");
        }
    }

    private String safeMessage(Throwable failure) {
        return failure.getMessage() == null
                ? failure.getClass().getSimpleName()
                : failure.getMessage();
    }
}
