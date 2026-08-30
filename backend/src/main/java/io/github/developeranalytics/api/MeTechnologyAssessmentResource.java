package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.technology.UserTechnologyAssessment;
import io.github.developeranalytics.persistence.technology.UserTechnologyAssessmentRepository;
import io.github.developeranalytics.service.technology.TechnologyEvidenceStrengthService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Path("/api/me/technology-assessments")
@Produces(MediaType.APPLICATION_JSON)
public class MeTechnologyAssessmentResource {

    @Inject
    CurrentUserService currentUserService;

    @Inject
    TechnologyEvidenceStrengthService strengthService;

    @Inject
    UserTechnologyAssessmentRepository assessments;

    @POST
    @Path("/recalculate")
    public Map<String, Object> recalculate(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        int count = strengthService.recalculate(current.user());

        return Map.of(
                "status", "COMPLETED",
                "technologiesCalculated", count
        );
    }

    @GET
    public List<Entry> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        return assessments.findForUser(current.user().getId())
                .stream()
                .map(Entry::from)
                .toList();
    }

    public record Entry(
            String technologyKey,
            String technologyName,
            String strength,
            int score,
            int repositoryCount,
            int evidenceCount,
            int independentEvidenceTypes,
            int recentRepositoryCount,
            OffsetDateTime firstObservedAt,
            OffsetDateTime lastObservedAt,
            OffsetDateTime calculatedAt,
            Map<String, Object> rationale
    ) {
        static Entry from(UserTechnologyAssessment assessment) {
            return new Entry(
                    assessment.getTechnology().getTechnologyKey(),
                    assessment.getTechnology().getDisplayName(),
                    assessment.getStrength().name(),
                    assessment.getScore(),
                    assessment.getRepositoryCount(),
                    assessment.getEvidenceCount(),
                    assessment.getIndependentEvidenceTypes(),
                    assessment.getRecentRepositoryCount(),
                    assessment.getFirstObservedAt(),
                    assessment.getLastObservedAt(),
                    assessment.getCalculatedAt(),
                    assessment.getRationale()
            );
        }
    }
}
