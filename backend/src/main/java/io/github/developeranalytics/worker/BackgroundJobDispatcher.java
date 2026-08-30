package io.github.developeranalytics.worker;
import io.github.developeranalytics.domain.job.BackgroundJob;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
@ApplicationScoped
public class BackgroundJobDispatcher {
    @Inject Instance<BackgroundJobHandler> handlers;
    public void dispatch(BackgroundJob job) throws Exception {
        for(BackgroundJobHandler h: handlers) if(h.jobType().equals(job.getJobType())) { h.handle(job); return; }
        throw new IllegalStateException("No handler registered for job type "+job.getJobType());
    }
}
