package io.github.developeranalytics.api.external;

import io.github.developeranalytics.auth.external.ExternalClientAuthService;
import io.github.developeranalytics.auth.external.ExternalClientAuthService.ExternalClientPrincipal;
import io.github.developeranalytics.domain.external.ExternalClientToken;
import io.github.developeranalytics.service.external.ExternalAnalysisApplicationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/me")
@Produces(ExternalAnalysisMediaType.VALUE)
public class ExternalAnalysisResource {

    @Inject ExternalClientAuthService externalAuth;
    @Inject ExternalAnalysisApplicationService externalAnalysis;

    @GET
    @Path("/profile")
    public Profile profile(
            @HeaderParam("Authorization") String authorization
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.PROFILE_READ
        );
        var result = externalAnalysis.profile(
                principal.user().getId(),
                principal.privacyScope());

        return new Profile(
                "v1",
                result.repositoryCount(),
                result.publicRepositoryCount(),
                result.privateRepositoryCount(),
                result.ownedRepositoryCount(),
                result.externalRepositoryCount(),
                result.contributionCount(),
                result.privacyProvenance(),
                result.topTechnologies().stream()
                        .map(this::technologySummary)
                        .toList(),
                result.topProjectTypes().stream()
                        .map(this::projectTypeSummary)
                        .toList()
        );
    }

    @GET
    @Path("/projects")
    public List<Project> projects(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("limit") @DefaultValue("50") int limit
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.PROJECTS_READ
        );

        return externalAnalysis.projects(
                        principal.user().getId(),
                        principal.privacyScope(),
                        limit)
                .stream()
                .map(project -> new Project(
                        project.id(),
                        project.name(),
                        project.visibility(),
                        project.ownership(),
                        project.lastActivityAt(),
                        project.projectTypes(),
                        project.technologies(),
                        project.excludedFromAiProfile()))
                .toList();
    }

    @GET
    @Path("/activity")
    public Activity activity(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("months") @DefaultValue("24") int months
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.ACTIVITY_READ
        );
        var result = externalAnalysis.activity(
                principal.user().getId(),
                principal.privacyScope(),
                months);

        return new Activity(
                result.contributionCount(),
                result.activeProjectCount(),
                result.contributionTypes(),
                result.monthly().stream()
                        .map(month -> new ActivityMonth(
                                month.month(),
                                month.contributions(),
                                month.activeProjects()))
                        .toList(),
                result.privacyProvenance());
    }

    @GET
    @Path("/technologies")
    public List<TechnologySummary> technologies(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("limit") @DefaultValue("30") int limit
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.TECHNOLOGIES_READ
        );

        return externalAnalysis.technologies(
                        principal.user().getId(),
                        principal.privacyScope(),
                        limit)
                .stream()
                .map(this::technologySummary)
                .toList();
    }

    @GET
    @Path("/project-types")
    public List<ProjectTypeSummary> projectTypes(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("limit") @DefaultValue("30") int limit
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.PROJECT_TYPES_READ
        );

        return externalAnalysis.projectTypes(
                        principal.user().getId(),
                        principal.privacyScope(),
                        limit)
                .stream()
                .map(this::projectTypeSummary)
                .toList();
    }

    @GET
    @Path("/contributions")
    public Contributions contributions(
            @HeaderParam("Authorization") String authorization
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.CONTRIBUTIONS_READ
        );
        var result = externalAnalysis.contributions(
                principal.user().getId(),
                principal.privacyScope());

        return new Contributions(
                result.total(),
                result.byType(),
                result.privacyProvenance());
    }

    @GET
    @Path("/evidence")
    public Evidence evidence(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("limit") @DefaultValue("50") int limit
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.EVIDENCE_READ
        );
        var result = externalAnalysis.evidence(
                principal.user().getId(),
                principal.privacyScope(),
                limit);

        return new Evidence(
                result.technologies().stream()
                        .map(this::technologyEvidence)
                        .toList(),
                result.projectTypes().stream()
                        .map(this::categoryEvidence)
                        .toList()
        );
    }

    private TechnologySummary technologySummary(
            ExternalAnalysisApplicationService.TechnologySummaryResult result
    ) {
        return new TechnologySummary(
                result.key(),
                result.name(),
                result.evidenceLevel(),
                result.evidenceScore(),
                result.projectCount(),
                result.firstObservedAt(),
                result.lastObservedAt(),
                result.privacyProvenance()
        );
    }

    private ProjectTypeSummary projectTypeSummary(
            ExternalAnalysisApplicationService.ProjectTypeSummaryResult result
    ) {
        return new ProjectTypeSummary(
                result.key(),
                result.name(),
                result.projectCount()
        );
    }

    private TechnologyEvidence technologyEvidence(
            ExternalAnalysisApplicationService.TechnologyEvidenceResult result
    ) {
        return new TechnologyEvidence(
                result.technologyKey(),
                result.evidenceType(),
                result.strength(),
                result.observations(),
                result.privacyProvenance()
        );
    }

    private CategoryEvidence categoryEvidence(
            ExternalAnalysisApplicationService.CategoryEvidenceResult result
    ) {
        return new CategoryEvidence(
                result.projectTypeKey(),
                result.source(),
                result.confidence(),
                result.observations(),
                result.privacyProvenance()
        );
    }

    public record Profile(
            String contractVersion,
            int repositoryCount,
            int publicRepositoryCount,
            int privateRepositoryCount,
            int ownedRepositoryCount,
            int externalRepositoryCount,
            int contributionCount,
            String privacyProvenance,
            List<TechnologySummary> topTechnologies,
            List<ProjectTypeSummary> topProjectTypes
    ) {}

    public record Project(
            UUID id,
            String name,
            String visibility,
            String ownership,
            OffsetDateTime lastActivityAt,
            List<String> projectTypes,
            List<String> technologies,
            boolean excludedFromAiProfile
    ) {}

    public record Activity(
            int contributionCount,
            int activeProjectCount,
            Map<String, Integer> contributionTypes,
            List<ActivityMonth> monthly,
            String privacyProvenance
    ) {}

    public record ActivityMonth(
            String month,
            int contributions,
            int activeProjects
    ) {}

    public record TechnologySummary(
            String key,
            String name,
            String evidenceLevel,
            int evidenceScore,
            int projectCount,
            OffsetDateTime firstObservedAt,
            OffsetDateTime lastObservedAt,
            String privacyProvenance
    ) {}

    public record ProjectTypeSummary(
            String key,
            String name,
            int projectCount
    ) {}

    public record Contributions(
            int total,
            Map<String, Integer> byType,
            String privacyProvenance
    ) {}

    public record Evidence(
            List<TechnologyEvidence> technologies,
            List<CategoryEvidence> projectTypes
    ) {}

    public record TechnologyEvidence(
            String technologyKey,
            String evidenceType,
            String strength,
            int observations,
            String privacyProvenance
    ) {}

    public record CategoryEvidence(
            String projectTypeKey,
            String source,
            String confidence,
            int observations,
            String privacyProvenance
    ) {}
}
