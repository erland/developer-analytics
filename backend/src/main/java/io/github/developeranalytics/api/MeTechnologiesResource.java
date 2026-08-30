package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.aggregate.TechnologyActivityMonth;
import io.github.developeranalytics.domain.technology.UserTechnologyAssessment;
import io.github.developeranalytics.persistence.technology.RepositoryTechnologyEvidenceRepository;
import io.github.developeranalytics.persistence.technology.TechnologyTimelineRepository;
import io.github.developeranalytics.persistence.technology.UserTechnologyAssessmentRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api/me/technologies")
@Produces(MediaType.APPLICATION_JSON)
public class MeTechnologiesResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    UserTechnologyAssessmentRepository assessments;

    @Inject
    TechnologyTimelineRepository timelines;

    @Inject
    RepositoryTechnologyEvidenceRepository evidence;

    @GET
    public List<Entry> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        Map<String, List<TechnologyActivityMonth>> monthsByTechnology =
                timelines.findMonthsForUser(current.user().getId())
                        .stream()
                        .collect(Collectors.groupingBy(
                                TechnologyActivityMonth::getTechnologyKey
                        ));

        return assessments.findForUser(current.user().getId())
                .stream()
                .map(assessment -> toEntry(
                        current.user().getId(),
                        assessment,
                        monthsByTechnology.getOrDefault(
                                assessment.getTechnology().getTechnologyKey(),
                                List.of()
                        )
                ))
                .toList();
    }

    private Entry toEntry(
            UUID userId,
            UserTechnologyAssessment assessment,
            List<TechnologyActivityMonth> months
    ) {
        var projects = evidence.findRepresentativeProjects(
                userId,
                assessment.getTechnology().getId(),
                5
        ).stream()
                .map(project -> new RepresentativeProject(
                        project.repositoryId(),
                        project.repositoryName(),
                        project.htmlUrl(),
                        project.visibility(),
                        project.ownershipRelation(),
                        project.lastActivityAt(),
                        project.evidenceCount()
                ))
                .toList();

        var timeline = months.stream()
                .sorted(Comparator.comparing(
                        TechnologyActivityMonth::getYearMonth
                ))
                .map(month -> new TimelinePoint(
                        month.getYearMonth(),
                        month.getRepositoryCount(),
                        month.getActivityCount()
                ))
                .toList();

        return new Entry(
                assessment.getTechnology().getTechnologyKey(),
                assessment.getTechnology().getDisplayName(),
                assessment.getTechnology().getCategory().name(),
                assessment.getStrength().name(),
                assessment.getScore(),
                assessment.getRepositoryCount(),
                assessment.getEvidenceCount(),
                assessment.getIndependentEvidenceTypes(),
                assessment.getFirstObservedAt(),
                assessment.getLastObservedAt(),
                assessment.getRecentRepositoryCount(),
                assessment.getRationale(),
                timeline,
                projects
        );
    }

    public record Entry(
            String technologyKey,
            String technologyName,
            String technologyCategory,
            String evidenceLevel,
            int evidenceScore,
            int projectCount,
            int evidenceCount,
            int independentEvidenceTypes,
            OffsetDateTime firstObservedAt,
            OffsetDateTime lastObservedAt,
            int recentProjectCount,
            Map<String, Object> rationale,
            List<TimelinePoint> timeline,
            List<RepresentativeProject> representativeProjects
    ) {}

    public record TimelinePoint(
            LocalDate month,
            int projectCount,
            int activityCount
    ) {}

    public record RepresentativeProject(
            UUID repositoryId,
            String repositoryName,
            String htmlUrl,
            String visibility,
            String ownershipRelation,
            OffsetDateTime lastActivityAt,
            int evidenceCount
    ) {}
}
