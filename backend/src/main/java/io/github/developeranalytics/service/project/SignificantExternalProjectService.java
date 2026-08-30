package io.github.developeranalytics.service.project;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.project.ProjectSignificanceAssessment;
import io.github.developeranalytics.persistence.project.ProjectSignificanceRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

@ApplicationScoped
public class SignificantExternalProjectService {

    public enum MatchReason {
        PROJECT_SIGNIFICANCE,
        USER_INVOLVEMENT,
        BOTH
    }

    @Inject
    ProjectSignificanceRepository assessments;

    public List<Result> find(AppUser user) {
        return assessments.findSignificantExternalProjects(user.getId())
                .stream()
                .map(this::map)
                .toList();
    }

    private Result map(ProjectSignificanceAssessment assessment) {
        boolean significantProject = isHigh(
                assessment.getSignificanceLevel()
        );
        boolean highInvolvement = isHigh(
                assessment.getInvolvementLevel()
        );

        MatchReason reason = significantProject && highInvolvement
                ? MatchReason.BOTH
                : significantProject
                    ? MatchReason.PROJECT_SIGNIFICANCE
                    : MatchReason.USER_INVOLVEMENT;

        return new Result(assessment, reason);
    }

    private boolean isHigh(ProjectSignificanceAssessment.Level level) {
        return level == ProjectSignificanceAssessment.Level.HIGH
                || level == ProjectSignificanceAssessment.Level.VERY_HIGH;
    }

    public record Result(
            ProjectSignificanceAssessment assessment,
            MatchReason reason
    ) {}
}
