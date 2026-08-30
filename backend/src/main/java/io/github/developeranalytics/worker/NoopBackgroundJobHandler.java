package io.github.developeranalytics.worker;
import io.github.developeranalytics.domain.job.BackgroundJob;
import jakarta.enterprise.context.ApplicationScoped;
@ApplicationScoped
public class NoopBackgroundJobHandler implements BackgroundJobHandler {
    public String jobType(){ return "NOOP"; }
    public void handle(BackgroundJob job) {}
}
