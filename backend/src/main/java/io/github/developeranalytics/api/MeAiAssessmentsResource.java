package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.auth.external.ExternalClientAuthService;
import io.github.developeranalytics.auth.external.ExternalClientAuthService.ExternalClientPrincipal;
import io.github.developeranalytics.domain.external.ExternalClientToken;
import io.github.developeranalytics.domain.external.ReturnedAiAssessment;
import io.github.developeranalytics.persistence.external.ExternalClientTokenRepository;
import io.github.developeranalytics.persistence.external.ReturnedAiAssessmentRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/me/ai-assessments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MeAiAssessmentsResource {

    @Inject CurrentUserService currentUserService;
    @Inject ExternalClientAuthService externalAuth;
    @Inject ExternalClientTokenRepository externalTokens;
    @Inject ReturnedAiAssessmentRepository assessments;

    @POST
    @Transactional
    public AssessmentResponse create(
            @HeaderParam("Authorization") String authorization,
            CreateRequest request
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.AI_ASSESSMENTS_WRITE
        );

        if (request == null ||
                request.analysisType() == null ||
                request.analysisType().isBlank() ||
                request.content() == null ||
                request.containsPrivateData() == null) {
            throw new BadRequestException(
                    "analysisType, content and containsPrivateData are required"
            );
        }

        validatePrivateDataClaim(
                principal.privacyScope(),
                request.containsPrivateData()
        );

        ExternalClientToken token = externalTokens
                .findByIdForUser(
                        principal.tokenId(),
                        principal.user().getId()
                )
                .orElseThrow(() ->
                        new NotAuthorizedException(
                                "External client token is unavailable"
                        ));

        ReturnedAiAssessment assessment =
                new ReturnedAiAssessment(
                        principal.user(),
                        token,
                        request.analysisType(),
                        principal.clientName(),
                        principal.privacyScope(),
                        request.content(),
                        request.containsPrivateData()
                );

        assessments.persist(assessment);
        return AssessmentResponse.from(assessment);
    }

    @GET
    @Transactional
    public List<AssessmentResponse> list(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @QueryParam("limit") @DefaultValue("50") int limit
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        int safeLimit = Math.max(1, Math.min(limit, 200));

        return assessments.findForUser(
                        current.user().getId(),
                        safeLimit
                )
                .stream()
                .map(AssessmentResponse::from)
                .toList();
    }

    @DELETE
    @Path("/{assessmentId}")
    @Transactional
    public void delete(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("assessmentId") UUID assessmentId
    ) {
        CurrentUser current =
                currentUserService.requireCurrentUser(sessionToken);

        ReturnedAiAssessment assessment = assessments
                .findByIdForUser(
                        assessmentId,
                        current.user().getId()
                )
                .orElseThrow(NotFoundException::new);

        assessments.delete(assessment);
    }

    private void validatePrivateDataClaim(
            ExternalClientToken.PrivacyScope privacyScope,
            boolean containsPrivateData
    ) {
        if (containsPrivateData &&
                !privacyScope.allowsPrivateAggregates()) {
            throw new ForbiddenException(
                    "This external client privacy scope cannot return private-data assessments"
            );
        }
    }

    public record CreateRequest(
            String analysisType,
            Map<String, Object> content,
            Boolean containsPrivateData
    ) {}

    public record AssessmentResponse(
            UUID id,
            String analysisType,
            String sourceClient,
            OffsetDateTime timestamp,
            String dataScope,
            Map<String, Object> content,
            boolean containsPrivateData
    ) {
        static AssessmentResponse from(
                ReturnedAiAssessment assessment
        ) {
            return new AssessmentResponse(
                    assessment.getId(),
                    assessment.getAnalysisType(),
                    assessment.getSourceClient(),
                    assessment.getCreatedAt(),
                    assessment.getDataScope().name(),
                    assessment.getContent(),
                    assessment.isContainsPrivateData()
            );
        }
    }
}
