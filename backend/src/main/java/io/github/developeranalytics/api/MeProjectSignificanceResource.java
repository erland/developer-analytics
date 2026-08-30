package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.project.ProjectSignificanceAssessment;
import io.github.developeranalytics.persistence.project.ProjectSignificanceRepository;
import io.github.developeranalytics.service.project.ProjectSignificanceService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Path("/api/me/project-significance")
@Produces(MediaType.APPLICATION_JSON)
public class MeProjectSignificanceResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    ProjectSignificanceService service;

    @Inject
    ProjectSignificanceRepository assessments;

    @POST
    @Path("/recalculate")
    public Map<String, Object> recalculate(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        int count = service.recalculate(current.user());

        return Map.of(
                "status", "COMPLETED",
                "repositoriesCalculated", count
        );
    }

    @GET
    public List<Entry> ranked(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        return assessments.findRanked(current.user().getId())
                .stream()
                .map(Entry::from)
                .toList();
    }

    public record Entry(
            java.util.UUID repositoryId,
            String repositoryName,
            String significanceLevel,
            int significanceScore,
            SignificanceBreakdown significance,
            String involvementLevel,
            int involvementScore,
            InvolvementBreakdown involvement,
            OffsetDateTime calculatedAt
    ) {
        static Entry from(ProjectSignificanceAssessment a) {
            return new Entry(
                    a.getRepository().getId(),
                    a.getRepository().getName(),
                    a.getSignificanceLevel().name(),
                    a.getSignificanceScore(),
                    new SignificanceBreakdown(
                            a.getPopularityScore(),
                            a.getContributorScore(),
                            a.getLongevityScore(),
                            a.getEcosystemScore(),
                            a.getActivityScore(),
                            a.getSignificanceRationale()
                    ),
                    a.getInvolvementLevel().name(),
                    a.getInvolvementScore(),
                    new InvolvementBreakdown(
                            a.getContributionScore(),
                            a.getInvolvementDurationScore(),
                            a.getInvolvementRecencyScore(),
                            a.getRelativeContributionScore(),
                            a.getInvolvementRationale()
                    ),
                    a.getCalculatedAt()
            );
        }
    }

    public record SignificanceBreakdown(
            int popularityScore,
            int contributorScore,
            int longevityScore,
            int ecosystemScore,
            int activityScore,
            Map<String, Object> rationale
    ) {}

    public record InvolvementBreakdown(
            int contributionScore,
            int durationScore,
            int recencyScore,
            int relativeContributionScore,
            Map<String, Object> rationale
    ) {}
}
