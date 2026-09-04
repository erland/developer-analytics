package io.github.developeranalytics.api.external;

import io.github.developeranalytics.auth.external.ExternalClientAuthService;
import io.github.developeranalytics.auth.external.ExternalClientAuthService.ExternalClientPrincipal;
import io.github.developeranalytics.domain.external.ExternalClientToken;
import io.github.developeranalytics.domain.model.*;
import io.github.developeranalytics.domain.technology.UserTechnologyAssessment;
import io.github.developeranalytics.persistence.project.ProjectTypeAnalyticsRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.persistence.technology.UserTechnologyAssessmentRepository;
import io.github.developeranalytics.service.external.ExternalAnalysisApplicationService;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;

import java.time.OffsetDateTime;
import java.util.*;

@Path("/api/me")
@Produces(ExternalAnalysisMediaType.VALUE)
public class ExternalAnalysisResource {

    @Inject ExternalClientAuthService externalAuth;
    @Inject SourceRepositoryRepository repositories;
    @Inject UserTechnologyAssessmentRepository technologyAssessments;
    @Inject ProjectTypeAnalyticsRepository projectTypes;
    @Inject EntityManager entityManager;
    @Inject ExternalAnalysisApplicationService externalAnalysis;

    @GET
    @Path("/profile")
    @Transactional
    public Profile profile(
            @HeaderParam("Authorization") String authorization
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.PROFILE_READ
        );
        UUID userId = principal.user().getId();

        List<SourceRepository> included = repositories.findAllForUser(userId)
                .stream()
                .filter(SourceRepository::isIncludedInAnalysis)
                .filter(repository ->
                        principal.privacyScope().allowsPrivateAggregates()
                        || repository.getVisibility() == RepositoryVisibility.PUBLIC)
                .toList();

        int publicRepositories = (int) included.stream()
                .filter(r -> r.getVisibility() == RepositoryVisibility.PUBLIC)
                .count();
        int privateRepositories = included.size() - publicRepositories;
        int ownedRepositories = (int) included.stream()
                .filter(r -> r.getOwnershipRelation() ==
                        RepositoryOwnershipRelation.OWNED_BY_USER)
                .count();

        Long contributionCount = entityManager.createQuery(
                "select count(c.id) from Contribution c " +
                "where c.user.id=:userId " +
                "and c.repository.includedInAnalysis=true",
                Long.class
        ).setParameter("userId", userId).getSingleResult();

        List<TechnologySummary> technologies =
                technologyAssessments.findForUser(userId).stream()
                        .filter(a ->
                                principal.privacyScope().allowsPrivateAggregates()
                                || a.getPrivacyProvenance() ==
                                        DataPrivacyProvenance.PUBLIC_ONLY)
                        .limit(10)
                        .map(this::technologySummary)
                        .toList();

        List<ProjectTypeSummary> categories =
                projectTypeSummaries(
                        userId,
                        principal.privacyScope(),
                        10
                );

        return new Profile(
                "v1",
                included.size(),
                publicRepositories,
                privateRepositories,
                ownedRepositories,
                included.size() - ownedRepositories,
                Math.toIntExact(contributionCount),
                privacyProvenance(publicRepositories, privateRepositories),
                technologies,
                categories
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
        UUID userId = principal.user().getId();
        int safeLimit = Math.max(1, Math.min(limit, 100));

        return technologyAssessments.findForUser(userId).stream()
                .filter(a ->
                        principal.privacyScope().allowsPrivateAggregates()
                        || a.getPrivacyProvenance() ==
                                DataPrivacyProvenance.PUBLIC_ONLY)
                .limit(safeLimit)
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
        UUID userId = principal.user().getId();
        int safeLimit = Math.max(1, Math.min(limit, 100));

        return projectTypeSummaries(
                userId,
                principal.privacyScope(),
                safeLimit
        );
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
    @Transactional
    public Evidence evidence(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("limit") @DefaultValue("50") int limit
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.EVIDENCE_READ
        );
        UUID userId = principal.user().getId();
        int safeLimit = Math.max(1, Math.min(limit, 200));

        List<TechnologyEvidence> technologyEvidence =
                entityManager.createQuery(
                        "select e.technology.technologyKey, " +
                        "e.evidenceType, e.strength, count(e), " +
                        "e.privacyProvenance " +
                        "from RepositoryTechnologyEvidence e " +
                        "where e.repository.user.id=:userId " +
                        "and e.repository.includedInAnalysis=true " +
                        "group by e.technology.technologyKey, " +
                        "e.evidenceType, e.strength, e.privacyProvenance " +
                        "order by count(e) desc",
                        Object[].class
                )
                .setParameter("userId", userId)
                .setMaxResults(safeLimit)
                .getResultList()
                .stream()
                .filter(row ->
                        principal.privacyScope().allowsPrivateAggregates()
                        || DataPrivacyProvenance.PUBLIC_ONLY.name()
                                .equals(row[4].toString()))
                .map(row -> new TechnologyEvidence(
                        (String) row[0],
                        row[1].toString(),
                        row[2].toString(),
                        ((Number) row[3]).intValue(),
                        row[4].toString()
                ))
                .toList();

        List<CategoryEvidence> categoryEvidence =
                entityManager.createQuery(
                        "select c.category.categoryKey, c.source, c.confidence, " +
                        "count(c), c.privacyProvenance " +
                        "from RepositoryProjectCategory c " +
                        "where c.repository.user.id=:userId " +
                        "and c.repository.includedInAnalysis=true " +
                        "group by c.category.categoryKey, c.source, " +
                        "c.confidence, c.privacyProvenance " +
                        "order by count(c) desc",
                        Object[].class
                )
                .setParameter("userId", userId)
                .setMaxResults(safeLimit)
                .getResultList()
                .stream()
                .filter(row ->
                        principal.privacyScope().allowsPrivateAggregates()
                        || DataPrivacyProvenance.PUBLIC_ONLY.name()
                                .equals(row[4].toString()))
                .map(row -> new CategoryEvidence(
                        (String) row[0],
                        row[1].toString(),
                        row[2].toString(),
                        ((Number) row[3]).intValue(),
                        row[4].toString()
                ))
                .toList();

        return new Evidence(
                technologyEvidence,
                categoryEvidence
        );
    }

    private List<ProjectTypeSummary> projectTypeSummaries(
            UUID userId,
            ExternalClientToken.PrivacyScope privacyScope,
            int limit
    ) {
        if (privacyScope.allowsPrivateAggregates()) {
            return projectTypes.categorySummaries(userId).stream()
                    .limit(limit)
                    .map(row -> new ProjectTypeSummary(
                            row.categoryKey(),
                            row.categoryName(),
                            row.projectCount()
                    ))
                    .toList();
        }

        return entityManager.createQuery(
                "select c.category.categoryKey, c.category.displayName, " +
                "count(distinct c.repository.id) " +
                "from RepositoryProjectCategory c " +
                "where c.repository.user.id=:userId " +
                "and c.repository.includedInAnalysis=true " +
                "and c.repository.visibility=:publicVisibility " +
                "group by c.category.categoryKey, c.category.displayName " +
                "order by count(distinct c.repository.id) desc",
                Object[].class
        )
        .setParameter("userId", userId)
        .setParameter(
                "publicVisibility",
                RepositoryVisibility.PUBLIC
        )
        .setMaxResults(limit)
        .getResultList()
        .stream()
        .map(row -> new ProjectTypeSummary(
                (String) row[0],
                (String) row[1],
                ((Number) row[2]).intValue()
        ))
        .toList();
    }

    private TechnologySummary technologySummary(
            UserTechnologyAssessment assessment
    ) {
        return new TechnologySummary(
                assessment.getTechnology().getTechnologyKey(),
                assessment.getTechnology().getDisplayName(),
                assessment.getStrength().name(),
                assessment.getScore(),
                assessment.getRepositoryCount(),
                assessment.getFirstObservedAt(),
                assessment.getLastObservedAt(),
                assessment.getPrivacyProvenance().name()
        );
    }

    private String privacyProvenance(
            int publicCount,
            int privateCount
    ) {
        return DataPrivacyProvenance
                .fromRepositoryCounts(publicCount, privateCount)
                .name();
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
