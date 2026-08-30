package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.persistence.repository.BackgroundJobRepository;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import java.time.OffsetDateTime;

@ApplicationScoped
public class BackgroundJobWorker {
    @Inject BackgroundJobRepository jobs;
    @Inject BackgroundJobDispatcher dispatcher;
    @ConfigProperty(name="developer-analytics.runtime-role", defaultValue="api") String runtimeRole;
    @ConfigProperty(name="developer-analytics.worker.id", defaultValue="worker-1") String workerId;

    @Scheduled(every="{developer-analytics.worker.poll-interval}", concurrentExecution=Scheduled.ConcurrentExecution.SKIP)
    void poll() {
        if(!"worker".equalsIgnoreCase(runtimeRole)) return;
        jobs.claimNext(workerId, OffsetDateTime.now()).ifPresent(this::execute);
    }

    @Transactional
    void execute(BackgroundJob job) {
        try { dispatcher.dispatch(job); job.complete(); }
        catch(Exception e) { job.retryOrFail(e.getMessage(), OffsetDateTime.now().plusSeconds(Math.min(300, 5L*Math.max(1,job.getAttemptCount())))); }
    }
}
