package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.service.activity.ActivityApplicationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Path("/api/me/activity")
@Produces(MediaType.APPLICATION_JSON)
public class MeActivityResource {
    @Inject CurrentUserService currentUserService;
    @Inject ActivityApplicationService activity;

    @GET
    public ActivityResponse get(@CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
                                @QueryParam("from") String from, @QueryParam("to") String to,
                                @QueryParam("year") Integer year, @QueryParam("month") String month,
                                @QueryParam("week") String week, @QueryParam("search") String search,
                                @QueryParam("ownership") String ownership, @QueryParam("visibility") String visibility,
                                @QueryParam("projectType") List<String> selectedProjectTypes,
                                @QueryParam("technology") List<String> technologiesFilter) {
        var current = currentUserService.requireCurrentUser(sessionToken);
        var period = AnalysisPeriod.resolve(from, to, year, month, week);
        return toResponse(activity.get(current.user().getId(), period.from(), period.to(), search, ownership, visibility,
                selectedProjectTypes, technologiesFilter));
    }

    private ActivityResponse toResponse(ActivityApplicationService.ActivityResult result) {
        return new ActivityResponse(
                result.commitCount(), result.activeProjects(), result.averageCommitSize(), result.medianCommitSize(),
                result.additions(), result.deletions(), result.firstActivityAt(), result.lastActivityAt(),
                result.commitsPerYear().stream().map(point -> new YearPoint(
                        point.year(), point.commits(), point.additions(), point.deletions(), point.changedLines(),
                        point.lineStatisticsCommitCount(), point.activeProjects(), point.projects())).toList(),
                result.commitsPerMonth().stream().map(point -> new MonthPoint(
                        point.month(), point.commits(), point.additions(), point.deletions(), point.changedLines(),
                        point.lineStatisticsCommitCount(), point.activeProjects(), point.projects())).toList(),
                result.commitsPerWeek().stream().map(point -> new WeekPoint(
                        point.week(), point.commits(), point.additions(), point.deletions(), point.changedLines(),
                        point.lineStatisticsCommitCount(), point.activeProjects(), point.projects())).toList(),
                result.projectsOverTime().stream().map(project -> new ProjectLifecycle(
                        project.repositoryId(), project.repositoryName(), project.firstActivityAt(), project.lastActivityAt(),
                        project.commits(), project.projectType(), project.technology(), project.projectTypes(), project.technologies(),
                        project.monthlyActivity().stream().map(MeActivityResource::toProjectPeriod).toList(),
                        project.weeklyActivity().stream().map(MeActivityResource::toProjectPeriod).toList())).toList(),
                result.commitSizeStatisticsAvailable(), result.lineStatisticsCommitCount());
    }

    private static ProjectPeriodActivity toProjectPeriod(ActivityApplicationService.ProjectPeriodActivity period) {
        return new ProjectPeriodActivity(period.period(), period.parentMonth(), period.commits(), period.additions(),
                period.deletions(), period.changedLines(), period.lineStatisticsCommitCount());
    }

    public record ActivityResponse(int commitCount,int activeProjects,double averageCommitSize,double medianCommitSize,long additions,long deletions,OffsetDateTime firstActivityAt,OffsetDateTime lastActivityAt,List<YearPoint> commitsPerYear,List<MonthPoint> commitsPerMonth,List<WeekPoint> commitsPerWeek,List<ProjectLifecycle> projectsOverTime,boolean commitSizeStatisticsAvailable,int lineStatisticsCommitCount) {}
    public record YearPoint(int year,int commits,long additions,long deletions,long changedLines,int lineStatisticsCommitCount,int activeProjects,List<String> projects) {}
    public record MonthPoint(String month,int commits,long additions,long deletions,long changedLines,int lineStatisticsCommitCount,int activeProjects,List<String> projects) {}
    public record WeekPoint(String week,int commits,long additions,long deletions,long changedLines,int lineStatisticsCommitCount,int activeProjects,List<String> projects) {}
    public record ProjectLifecycle(UUID repositoryId,String repositoryName,OffsetDateTime firstActivityAt,OffsetDateTime lastActivityAt,int commits,String projectType,String technology,List<String> projectTypes,List<String> technologies,List<ProjectPeriodActivity> monthlyActivity,List<ProjectPeriodActivity> weeklyActivity) {}
    public record ProjectPeriodActivity(String period,String parentMonth,int commits,long additions,long deletions,long changedLines,int lineStatisticsCommitCount) {}
}
