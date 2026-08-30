package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.project.ProjectSignificanceAssessment;
import io.github.developeranalytics.service.project.SignificantExternalProjectService;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/me/significant-external-projects")
@Produces(MediaType.APPLICATION_JSON)
public class MeSignificantExternalProjectsResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    SignificantExternalProjectService service;

    @GET
    public List<Entry> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        return service.find(current.user())
                .stream()
                .map(Entry::from)
                .toList();
    }

    public record Entry(
            UUID repositoryId,
            String repositoryName,
            String repositoryUrl,
            String ownershipRelation,
            String matchReason,
            String significanceLevel,
            int significanceScore,
            Map<String, Object> significanceEvidence,
            String involvementLevel,
            int involvementScore,
            Map<String, Object> involvementEvidence,
            OffsetDateTime calculatedAt
    ) {
        static Entry from(
                SignificantExternalProjectService.Result result
        ) {
            ProjectSignificanceAssessment assessment =
                    result.assessment();

            return new Entry(
                    assessment.getRepository().getId(),
                    assessment.getRepository().getName(),
                    assessment.getRepository().getHtmlUrl(),
                    assessment.getRepository()
                            .getOwnershipRelation()
                            .name(),
                    result.reason().name(),
                    assessment.getSignificanceLevel().name(),
                    assessment.getSignificanceScore(),
                    assessment.getSignificanceRationale(),
                    assessment.getInvolvementLevel().name(),
                    assessment.getInvolvementScore(),
                    assessment.getInvolvementRationale(),
                    assessment.getCalculatedAt()
            );
        }
    }
}
