package io.github.developeranalytics.api.external;

import io.github.developeranalytics.auth.external.ExternalClientAuthService;
import io.github.developeranalytics.auth.external.ExternalClientAuthService.ExternalClientPrincipal;
import io.github.developeranalytics.domain.external.ExternalClientToken;
import io.github.developeranalytics.domain.model.*;
import io.github.developeranalytics.domain.technology.UserTechnologyAssessment;
import io.github.developeranalytics.persistence.correction.UserAnalysisCorrectionRepository;
import io.github.developeranalytics.persistence.project.ProjectTypeAnalyticsRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.persistence.technology.UserTechnologyAssessmentRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Path("/api/me")
@Produces(ExternalAnalysisMediaType.VALUE)
public class ExternalAnalysisResource {

    @Inject ExternalClientAuthService externalAuth;
    @Inject SourceRepositoryRepository repositories;
    @Inject UserTechnologyAssessmentRepository technologyAssessments;
    @Inject ProjectTypeAnalyticsRepository projectTypes;
    @Inject UserAnalysisCorrectionRepository corrections;
    @Inject EntityManager entityManager;

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
                        .filter(a -> !isTechnologySuppressed(
                                userId,
                                a.getTechnology().getTechnologyKey()
                        ))
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
    @Transactional
    public List<Project> projects(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("limit") @DefaultValue("50") int limit
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.PROJECTS_READ
        );
        UUID userId = principal.user().getId();
        int safeLimit = Math.max(1, Math.min(limit, 200));

        return repositories.findAllForUser(userId).stream()
                .filter(SourceRepository::isIncludedInAnalysis)
                .filter(repository ->
                        principal.privacyScope().allowsPrivateProjectDetail()
                        || repository.getVisibility() == RepositoryVisibility.PUBLIC)
                .limit(safeLimit)
                .map(repository -> new Project(
                        repository.getId(),
                        repository.getName(),
                        repository.getVisibility().name(),
                        repository.getOwnershipRelation().name(),
                        repository.getLastActivityAt(),
                        categoryKeys(userId, repository.getId()),
                        technologyKeys(userId, repository.getId()),
                        excludedFromAiProfile(userId, repository.getId())
                ))
                .toList();
    }

    @GET
    @Path("/activity")
    @Transactional
    public Activity activity(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("months") @DefaultValue("24") int months
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.ACTIVITY_READ
        );
        UUID userId = principal.user().getId();
        int safeMonths = Math.max(1, Math.min(months, 120));
        OffsetDateTime threshold = OffsetDateTime.now()
                .minusMonths(safeMonths);

        String visibilityClause =
                principal.privacyScope().allowsPrivateAggregates()
                        ? ""
                        : " and c.repository.visibility=:publicVisibility ";

        var activityQuery = entityManager.createQuery(
                "select c.occurredAt, c.type, c.repository.id, " +
                "c.repository.visibility from Contribution c " +
                "where c.user.id=:userId " +
                "and c.repository.includedInAnalysis=true " +
                "and c.occurredAt>=:threshold " +
                visibilityClause +
                "order by c.occurredAt",
                Object[].class
        )
        .setParameter("userId", userId)
        .setParameter("threshold", threshold);

        if (!principal.privacyScope().allowsPrivateAggregates()) {
            activityQuery.setParameter(
                    "publicVisibility",
                    RepositoryVisibility.PUBLIC
            );
        }

        List<Object[]> rows = activityQuery.getResultList();

        Map<YearMonth, MutableMonth> monthly = new TreeMap<>();
        EnumMap<Contribution.Type, Integer> totals =
                new EnumMap<>(Contribution.Type.class);
        Set<UUID> activeProjects = new HashSet<>();
        int publicRows = 0;
        int privateRows = 0;

        for (Object[] row : rows) {
            OffsetDateTime occurredAt = (OffsetDateTime) row[0];
            Contribution.Type type = (Contribution.Type) row[1];
            UUID repositoryId = (UUID) row[2];
            RepositoryVisibility visibility =
                    (RepositoryVisibility) row[3];

            totals.merge(type, 1, Integer::sum);
            activeProjects.add(repositoryId);
            if (visibility == RepositoryVisibility.PRIVATE) privateRows++;
            else publicRows++;

            MutableMonth month = monthly.computeIfAbsent(
                    YearMonth.from(occurredAt),
                    ignored -> new MutableMonth()
            );
            month.contributions++;
            month.projects.add(repositoryId);
        }

        return new Activity(
                rows.size(),
                activeProjects.size(),
                contributionTotals(totals),
                monthly.entrySet().stream()
                        .map(entry -> new ActivityMonth(
                                entry.getKey().toString(),
                                entry.getValue().contributions,
                                entry.getValue().projects.size()
                        ))
                        .toList(),
                privacyProvenance(publicRows, privateRows)
        );
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
                .filter(a -> !isTechnologySuppressed(
                        userId,
                        a.getTechnology().getTechnologyKey()
                ))
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
    @Transactional
    public Contributions contributions(
            @HeaderParam("Authorization") String authorization
    ) {
        ExternalClientPrincipal principal = externalAuth.require(
                authorization,
                ExternalClientToken.Scope.CONTRIBUTIONS_READ
        );
        UUID userId = principal.user().getId();

        String contributionVisibilityClause =
                principal.privacyScope().allowsPrivateAggregates()
                        ? ""
                        : " and c.repository.visibility=:publicVisibility ";

        var contributionQuery = entityManager.createQuery(
                "select c.type, count(c.id), c.repository.visibility " +
                "from Contribution c " +
                "where c.user.id=:userId " +
                "and c.repository.includedInAnalysis=true " +
                contributionVisibilityClause +
                "group by c.type, c.repository.visibility",
                Object[].class
        )
        .setParameter("userId", userId);

        if (!principal.privacyScope().allowsPrivateAggregates()) {
            contributionQuery.setParameter(
                    "publicVisibility",
                    RepositoryVisibility.PUBLIC
            );
        }

        List<Object[]> rows = contributionQuery.getResultList();

        EnumMap<Contribution.Type, Integer> totals =
                new EnumMap<>(Contribution.Type.class);
        int publicCount = 0;
        int privateCount = 0;

        for (Object[] row : rows) {
            Contribution.Type type = (Contribution.Type) row[0];
            int count = ((Number) row[1]).intValue();
            RepositoryVisibility visibility =
                    (RepositoryVisibility) row[2];

            totals.merge(type, count, Integer::sum);
            if (visibility == RepositoryVisibility.PRIVATE) {
                privateCount += count;
            } else {
                publicCount += count;
            }
        }

        return new Contributions(
                totals.values().stream().mapToInt(Integer::intValue).sum(),
                contributionTotals(totals),
                privacyProvenance(publicCount, privateCount)
        );
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
                .filter(row -> !isTechnologySuppressed(
                        userId,
                        (String) row[0]
                ))
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
                        "and not exists (" +
                        "select 1 from UserAnalysisCorrection correction " +
                        "where correction.user.id=:userId " +
                        "and correction.repository.id=c.repository.id " +
                        "and correction.type=" +
                        "io.github.developeranalytics.domain.correction." +
                        "UserAnalysisCorrection.Type.PROJECT_CATEGORY_REJECTED " +
                        "and correction.correctionKey=c.category.categoryKey) " +
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
            "and not exists (" +
            "select 1 from UserAnalysisCorrection correction " +
            "where correction.user.id=:userId " +
            "and correction.repository.id=c.repository.id " +
            "and correction.type=" +
            "io.github.developeranalytics.domain.correction." +
            "UserAnalysisCorrection.Type.PROJECT_CATEGORY_REJECTED " +
            "and correction.correctionKey=c.category.categoryKey) " +
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

    private List<String> categoryKeys(UUID userId, UUID repositoryId) {
        return entityManager.createQuery(
                "select distinct c.category.categoryKey " +
                "from RepositoryProjectCategory c " +
                "where c.repository.id=:repositoryId " +
                "and not exists (" +
                "select 1 from UserAnalysisCorrection correction " +
                "where correction.user.id=:userId " +
                "and correction.repository.id=:repositoryId " +
                "and correction.type=" +
                "io.github.developeranalytics.domain.correction." +
                "UserAnalysisCorrection.Type.PROJECT_CATEGORY_REJECTED " +
                "and correction.correctionKey=c.category.categoryKey)",
                String.class
        )
        .setParameter("repositoryId", repositoryId)
        .setParameter("userId", userId)
        .getResultList();
    }

    private List<String> technologyKeys(UUID userId, UUID repositoryId) {
        return entityManager.createQuery(
                "select distinct e.technology.technologyKey " +
                "from RepositoryTechnologyEvidence e " +
                "where e.repository.id=:repositoryId " +
                "order by e.technology.technologyKey",
                String.class
        )
        .setParameter("repositoryId", repositoryId)
        .getResultList()
        .stream()
        .filter(key -> !isTechnologySuppressed(userId, key))
        .toList();
    }

    private boolean isTechnologySuppressed(UUID userId, String key) {
        return corrections.exists(
                userId,
                null,
                io.github.developeranalytics.domain.correction.
                        UserAnalysisCorrection.Type.TECHNOLOGY_INFERENCE_SUPPRESSED,
                key
        );
    }

    private boolean excludedFromAiProfile(
            UUID userId,
            UUID repositoryId
    ) {
        return corrections.exists(
                userId,
                repositoryId,
                io.github.developeranalytics.domain.correction.
                        UserAnalysisCorrection.Type.PROJECT_EXCLUDED_FROM_AI_PROFILE,
                null
        );
    }

    private Map<String, Integer> contributionTotals(
            Map<Contribution.Type, Integer> totals
    ) {
        Map<String, Integer> result = new LinkedHashMap<>();
        for (Contribution.Type type : Contribution.Type.values()) {
            result.put(
                    type.name().toLowerCase(Locale.ROOT),
                    totals.getOrDefault(type, 0)
            );
        }
        return result;
    }

    private String privacyProvenance(
            int publicCount,
            int privateCount
    ) {
        return DataPrivacyProvenance
                .fromRepositoryCounts(publicCount, privateCount)
                .name();
    }

    private static final class MutableMonth {
        int contributions;
        final Set<UUID> projects = new HashSet<>();
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
