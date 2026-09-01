package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.service.technology.TechnologyTimelineService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TechnologyTimelineRecalculationJobHandler implements BackgroundJobHandler {
    public static final String JOB_TYPE = "TECHNOLOGY_TIMELINE_RECALCULATION";

    @Inject TechnologyTimelineService service;

    @Override public String jobType() { return JOB_TYPE; }

    @Override
    public void handle(BackgroundJob job) {
        if (job.getUser() == null) {
            throw new IllegalStateException("Technology timeline recalculation requires a user");
        }
        service.recalculate(job.getUser());
    }
}
