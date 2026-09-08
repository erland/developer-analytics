package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.persistence.project.ProjectTypeAnalyticsRepository;
import io.github.developeranalytics.service.activity.ChangeKindDimensionActivityService;
import io.github.developeranalytics.service.change.ChangeKindSelection;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api/me")
@Produces(MediaType.APPLICATION_JSON)
public class MeProjectTypesResource {

    @Inject CurrentUserService currentUserService;
    @Inject ProjectTypeAnalyticsRepository analytics;
    @Inject ChangeKindDimensionActivityService filteredActivity;

    @GET
    @Path("/project-types")
    public List<Entry> list(@CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
                            @QueryParam("changeKinds") List<String> rawChangeKinds) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        final var changeKinds = parseChangeKinds(rawChangeKinds);

        if (!ChangeKindSelection.isAll(changeKinds)) {
            Map<String, List<ChangeKindDimensionActivityService.MetricRow>> activity =
                    filteredActivity.categoryActivity(current.user().getId(), changeKinds);
            return analytics.categorySummaries(current.user().getId()).stream()
                    .map(summary -> toFilteredEntry(current.user().getId(), summary,
                            activity.getOrDefault(summary.categoryKey(), List.of())))
                    .toList();
        }

        Map<String, List<ProjectTypeAnalyticsRepository.CategoryActivityRow>> activity =
                analytics.categoryActivity(current.user().getId()).stream()
                        .collect(Collectors.groupingBy(ProjectTypeAnalyticsRepository.CategoryActivityRow::categoryKey));

        return analytics.categorySummaries(current.user().getId()).stream()
                .map(summary -> toEntry(current.user().getId(), summary,
                        activity.getOrDefault(summary.categoryKey(), List.of())))
                .toList();
    }

    private Set<io.github.developeranalytics.domain.change.ChangeKind> parseChangeKinds(List<String> raw) {
        try { return ChangeKindSelection.parse(raw); }
        catch (IllegalArgumentException error) { throw new BadRequestException(error.getMessage()); }
    }

    private Entry toFilteredEntry(UUID userId,
                                  ProjectTypeAnalyticsRepository.CategorySummaryRow summary,
                                  List<ChangeKindDimensionActivityService.MetricRow> rows) {
        int totalCommits = rows.stream().mapToInt(ChangeKindDimensionActivityService.MetricRow::commits).sum();
        List<TimelinePoint> timeline = rows.stream()
                .filter(row -> row.commits() > 0 || row.changedLines() > 0 || row.activeProjectCount() > 0)
                .map(row -> new TimelinePoint(row.month(), row.commits(), row.changedLines(),
                        row.lineStatisticsCommitCount(), row.activeProjectCount()))
                .toList();
        return new Entry(summary.categoryKey(), summary.categoryName(), summary.projectCount(),
                totalCommits, timeline, representativeProjects(userId, summary.categoryKey()));
    }

    private Entry toEntry(UUID userId,
                          ProjectTypeAnalyticsRepository.CategorySummaryRow summary,
                          List<ProjectTypeAnalyticsRepository.CategoryActivityRow> rows) {
        int totalCommits = rows.stream().mapToInt(ProjectTypeAnalyticsRepository.CategoryActivityRow::commitCount).sum();
        List<TimelinePoint> timeline = rows.stream()
                .filter(MeProjectTypesResource::hasActivity)
                .sorted(Comparator.comparing(ProjectTypeAnalyticsRepository.CategoryActivityRow::month))
                .map(row -> new TimelinePoint(
                        row.month(), row.commitCount(), row.changedLines(),
                        row.lineStatisticsCommitCount(), row.activeProjectCount()))
                .toList();
        return new Entry(summary.categoryKey(), summary.categoryName(), summary.projectCount(),
                totalCommits, timeline, representativeProjects(userId, summary.categoryKey()));
    }

    private List<RepresentativeProject> representativeProjects(UUID userId, String categoryKey) {
        return analytics.representativeProjects(userId, categoryKey, 500).stream()
                .map(project -> new RepresentativeProject(
                        project.repositoryId(), project.repositoryName(), project.htmlUrl(), project.visibility(),
                        project.ownershipRelation(), project.lastActivityAt(), project.contributionCount()))
                .toList();
    }

    static boolean hasActivity(ProjectTypeAnalyticsRepository.CategoryActivityRow row) {
        return row.commitCount() > 0 || row.changedLines() > 0 || row.lineStatisticsCommitCount() > 0 || row.activeProjectCount() > 0;
    }

    public record Entry(String categoryKey,String categoryName,int projectCount,int activityCount,List<TimelinePoint> timeline,List<RepresentativeProject> representativeProjects) {}
    public record TimelinePoint(String month,int commits,long changedLines,int lineStatisticsCommitCount,int activeProjectCount) {}
    public record RepresentativeProject(UUID repositoryId,String repositoryName,String htmlUrl,String visibility,String ownershipRelation,OffsetDateTime lastActivityAt,int contributionCount) {}
}
