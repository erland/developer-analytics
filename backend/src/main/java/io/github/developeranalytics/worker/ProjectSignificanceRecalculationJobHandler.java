package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.service.project.ProjectSignificanceService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProjectSignificanceRecalculationJobHandler implements BackgroundJobHandler {
    public static final String JOB_TYPE = "PROJECT_SIGNIFICANCE_RECALCULATION";

    @Inject ProjectSignificanceService service;

    @Override public String jobType() { return JOB_TYPE; }

    @Override
    public void handle(BackgroundJob job) {
        if (job.getUser() == null) {
            throw new IllegalStateException("Project significance recalculation requires a user");
        }
        service.recalculate(job.getUser());
    }
}
