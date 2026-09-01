package io.github.developeranalytics.worker;

import io.github.developeranalytics.domain.job.BackgroundJob;
import io.github.developeranalytics.service.technology.TechnologyEvidenceStrengthService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TechnologyAssessmentRecalculationJobHandler implements BackgroundJobHandler {
    public static final String JOB_TYPE = "TECHNOLOGY_ASSESSMENT_RECALCULATION";

    @Inject TechnologyEvidenceStrengthService service;

    @Override public String jobType() { return JOB_TYPE; }

    @Override
    public void handle(BackgroundJob job) {
        if (job.getUser() == null) {
            throw new IllegalStateException("Technology assessment recalculation requires a user");
        }
        service.recalculate(job.getUser());
    }
}
