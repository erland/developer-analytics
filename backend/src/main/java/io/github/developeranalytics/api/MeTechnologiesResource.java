package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.technology.UserTechnologyAssessment;
import io.github.developeranalytics.persistence.technology.RepositoryTechnologyEvidenceRepository;
import io.github.developeranalytics.persistence.technology.TechnologyTimelineRepository;
import io.github.developeranalytics.persistence.technology.UserTechnologyAssessmentRepository;
import io.github.developeranalytics.service.correction.UserCorrectionService;
import io.github.developeranalytics.service.technology.TechnologyEvidenceStrengthService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api/me/technologies")
@Produces(MediaType.APPLICATION_JSON)
public class MeTechnologiesResource {
    private static final Logger LOG = Logger.getLogger(MeTechnologiesResource.class);

    @Inject CurrentUserService currentUserService;
    @Inject UserTechnologyAssessmentRepository assessments;
    @Inject TechnologyTimelineRepository timelines;
    @Inject RepositoryTechnologyEvidenceRepository evidence;
    @Inject UserCorrectionService corrections;
    @Inject TechnologyEvidenceStrengthService strengthService;

    @GET
    @Transactional
    public List<Entry> list(@CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        UUID userId = current.user().getId();
        List<UserTechnologyAssessment> currentAssessments = assessments.findForUser(userId);
        if (currentAssessments.isEmpty()) {
            try {
                strengthService.recalculate(current.user());
                currentAssessments = assessments.findForUser(userId);
            } catch (RuntimeException exception) {
                LOG.errorf(exception,
                        "event=technology_assessment_recalculation_failed userId=%s errorType=%s errorMessage=%s",
                        userId, exception.getClass().getSimpleName(), exception.getMessage());
                throw exception;
            }
        }

        Map<String, List<TechnologyTimelineRepository.MetricActivityRow>> activityByTechnology =
                timelines.calculateMetricActivity(userId).stream()
                        .collect(Collectors.groupingBy(TechnologyTimelineRepository.MetricActivityRow::technologyKey));
        try {
            return currentAssessments.stream()
                    .filter(assessment -> !corrections.isTechnologySuppressed(
                            userId, assessment.getTechnology().getTechnologyKey()))
                    .map(assessment -> toEntry(userId, assessment,
                            activityByTechnology.getOrDefault(
                                    assessment.getTechnology().getTechnologyKey(), List.of())))
                    .toList();
        } catch (RuntimeException exception) {
            LOG.errorf(exception,
                    "event=technology_response_mapping_failed userId=%s assessmentCount=%d errorType=%s errorMessage=%s",
                    userId, currentAssessments.size(), exception.getClass().getSimpleName(), exception.getMessage());
            throw exception;
        }
    }

    private Entry toEntry(UUID userId, UserTechnologyAssessment assessment,
                          List<TechnologyTimelineRepository.MetricActivityRow> activity) {
        var projects = evidence.findRepresentativeProjects(
                        userId, assessment.getTechnology().getId(), 1000).stream()
                .map(project -> new RepresentativeProject(
                        project.repositoryId(), project.repositoryName(), project.htmlUrl(), project.visibility(),
                        project.ownershipRelation(), project.lastActivityAt(), project.evidenceCount()))
                .toList();
        var timeline = activity.stream()
                .sorted(Comparator.comparing(TechnologyTimelineRepository.MetricActivityRow::month))
                .map(month -> new TimelinePoint(month.month(), month.commits(), month.changedLines(),
                        month.lineStatisticsCommitCount(), month.activeProjectCount()))
                .toList();
        return new Entry(
                assessment.getTechnology().getTechnologyKey(), assessment.getTechnology().getDisplayName(),
                assessment.getTechnology().getCategory().name(), assessment.getStrength().name(),
                assessment.getScore(), assessment.getRepositoryCount(), assessment.getEvidenceCount(),
                assessment.getIndependentEvidenceTypes(), assessment.getFirstObservedAt(),
                assessment.getLastObservedAt(), assessment.getRecentRepositoryCount(),
                assessment.getPrivacyProvenance().name(), assessment.getRationale(), timeline, projects);
    }

    public record Entry(String technologyKey, String technologyName, String technologyCategory,
                        String evidenceLevel, int evidenceScore, int projectCount, int evidenceCount,
                        int independentEvidenceTypes, OffsetDateTime firstObservedAt, OffsetDateTime lastObservedAt,
                        int recentProjectCount, String privacyProvenance, Map<String, Object> rationale,
                        List<TimelinePoint> timeline, List<RepresentativeProject> representativeProjects) {}
    public record TimelinePoint(String month, int commits, long changedLines,
                                int lineStatisticsCommitCount, int projectCount) {}
    public record RepresentativeProject(UUID repositoryId, String repositoryName, String htmlUrl,
                                        String visibility, String ownershipRelation,
                                        OffsetDateTime lastActivityAt, int evidenceCount) {}
}
