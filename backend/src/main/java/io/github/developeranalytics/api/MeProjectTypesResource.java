package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.persistence.project.ProjectTypeAnalyticsRepository;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api/me/project-types")
@Produces(MediaType.APPLICATION_JSON)
public class MeProjectTypesResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    ProjectTypeAnalyticsRepository analytics;

    @GET
    public List<Entry> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        Map<String, List<ProjectTypeAnalyticsRepository.CategoryActivityRow>> activity =
                analytics.categoryActivity(current.user().getId())
                        .stream()
                        .collect(Collectors.groupingBy(
                                ProjectTypeAnalyticsRepository.CategoryActivityRow::categoryKey
                        ));

        return analytics.categorySummaries(current.user().getId())
                .stream()
                .map(summary -> toEntry(
                        current.user().getId(),
                        summary,
                        activity.getOrDefault(
                                summary.categoryKey(),
                                List.of()
                        )
                ))
                .toList();
    }

    private Entry toEntry(
            UUID userId,
            ProjectTypeAnalyticsRepository.CategorySummaryRow summary,
            List<ProjectTypeAnalyticsRepository.CategoryActivityRow> rows
    ) {
        int totalActivity = rows.stream()
                .mapToInt(
                        ProjectTypeAnalyticsRepository.CategoryActivityRow::activityCount
                )
                .sum();

        List<TimelinePoint> timeline = rows.stream()
                .map(row -> new TimelinePoint(
                        row.month(),
                        row.activityCount(),
                        row.activeProjectCount()
                ))
                .toList();

        List<RepresentativeProject> representatives =
                analytics.representativeProjects(
                        userId,
                        summary.categoryKey(),
                        5
                ).stream()
                .map(project -> new RepresentativeProject(
                        project.repositoryId(),
                        project.repositoryName(),
                        project.htmlUrl(),
                        project.visibility(),
                        project.ownershipRelation(),
                        project.lastActivityAt(),
                        project.contributionCount()
                ))
                .toList();

        return new Entry(
                summary.categoryKey(),
                summary.categoryName(),
                summary.projectCount(),
                totalActivity,
                timeline,
                representatives
        );
    }

    public record Entry(
            String categoryKey,
            String categoryName,
            int projectCount,
            int activityCount,
            List<TimelinePoint> timeline,
            List<RepresentativeProject> representativeProjects
    ) {}

    public record TimelinePoint(
            String month,
            int activityCount,
            int activeProjectCount
    ) {}

    public record RepresentativeProject(
            UUID repositoryId,
            String repositoryName,
            String htmlUrl,
            String visibility,
            String ownershipRelation,
            OffsetDateTime lastActivityAt,
            int contributionCount
    ) {}
}
