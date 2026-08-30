package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.domain.project.ProjectSignificanceAssessment;
import io.github.developeranalytics.persistence.project.ProjectSignificanceRepository;
import io.github.developeranalytics.persistence.project.RepositoryProjectCategoryRepository;
import io.github.developeranalytics.persistence.repository.ContributionRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.persistence.technology.RepositoryTechnologyEvidenceRepository;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;

@Path("/api/me/projects/{repositoryId}")
@Produces(MediaType.APPLICATION_JSON)
public class MeProjectDetailResource {

    @Inject CurrentUserService currentUserService;
    @Inject SourceRepositoryRepository repositories;
    @Inject RepositoryTechnologyEvidenceRepository technologyEvidence;
    @Inject RepositoryProjectCategoryRepository categoryAssignments;
    @Inject ProjectSignificanceRepository significance;
    @Inject EntityManager entityManager;

    @GET
    @Transactional
    public Detail get(
            @CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
            @PathParam("repositoryId") UUID repositoryId
    ) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);

        SourceRepository repository = repositories
                .findByIdForUser(repositoryId, current.user().getId())
                .orElseThrow(NotFoundException::new);

        var technologies = technologyEvidence
                .findForRepository(current.user().getId(), repositoryId)
                .stream()
                .map(evidence -> new TechnologyEvidence(
                        evidence.getTechnology().getTechnologyKey(),
                        evidence.getTechnology().getDisplayName(),
                        evidence.getEvidenceType().name(),
                        evidence.getStrength().name(),
                        evidence.getSourceValue(),
                        evidence.getMeasuredValue(),
                        evidence.getObservedAt()
                ))
                .toList();

        var categories = categoryAssignments.findForRepository(repositoryId)
                .stream()
                .map(assignment -> new Category(
                        assignment.getCategory().getCategoryKey(),
                        assignment.getCategory().getDisplayName(),
                        assignment.getSource().name(),
                        assignment.getConfidence().name(),
                        assignment.getRationale()
                ))
                .toList();

        ProjectSignificanceAssessment assessment = significance
                .find(current.user().getId(), repositoryId)
                .orElse(null);

        var activity = loadActivity(current.user().getId(), repositoryId);

        return new Detail(
                new Metadata(
                        repository.getId(),
                        repository.getProvider(),
                        repository.getName(),
                        repository.getFullName(),
                        repository.getDescription(),
                        repository.getHtmlUrl(),
                        repository.getVisibility().name(),
                        repository.getOwnershipRelation().name(),
                        repository.getOwnerLogin(),
                        repository.isFork(),
                        repository.isArchived(),
                        repository.getTopics(),
                        repository.getLastActivityAt()
                ),
                activity,
                technologies,
                categories,
                assessment == null ? null : new Assessment(
                        assessment.getSignificanceLevel().name(),
                        assessment.getSignificanceScore(),
                        assessment.getSignificanceRationale(),
                        assessment.getInvolvementLevel().name(),
                        assessment.getInvolvementScore(),
                        assessment.getInvolvementRationale(),
                        assessment.getCalculatedAt()
                ),
                new Synchronisation(
                        repository.getSyncStatus().name(),
                        repository.getLastSeenAt(),
                        repository.getSyncError()
                )
        );
    }

    private Activity loadActivity(UUID userId, UUID repositoryId) {
        List<Object[]> rows = entityManager.createQuery(
                "select c.occurredAt, c.type, c.additions, c.deletions " +
                "from Contribution c " +
                "where c.user.id=:userId and c.repository.id=:repositoryId " +
                "order by c.occurredAt",
                Object[].class)
            .setParameter("userId", userId)
            .setParameter("repositoryId", repositoryId)
            .getResultList();

        Map<YearMonth, Integer> commitsPerMonth = new TreeMap<>();
        int commits = 0;
        int pullRequests = 0;
        int reviews = 0;
        int issues = 0;
        long additions = 0;
        long deletions = 0;
        OffsetDateTime first = null;
        OffsetDateTime last = null;

        for (Object[] row : rows) {
            OffsetDateTime occurredAt = (OffsetDateTime) row[0];
            var type = (io.github.developeranalytics.domain.model.Contribution.Type) row[1];
            Integer add = (Integer) row[2];
            Integer del = (Integer) row[3];

            if (first == null || occurredAt.isBefore(first)) first = occurredAt;
            if (last == null || occurredAt.isAfter(last)) last = occurredAt;

            switch (type) {
                case COMMIT -> {
                    commits++;
                    commitsPerMonth.merge(YearMonth.from(occurredAt), 1, Integer::sum);
                    additions += add == null ? 0 : add;
                    deletions += del == null ? 0 : del;
                }
                case PULL_REQUEST -> pullRequests++;
                case REVIEW -> reviews++;
                case ISSUE -> issues++;
                default -> { }
            }
        }

        List<ActivityPoint> timeline = commitsPerMonth.entrySet().stream()
                .map(entry -> new ActivityPoint(
                        entry.getKey().toString(),
                        entry.getValue()
                ))
                .toList();

        return new Activity(
                commits,
                pullRequests,
                reviews,
                issues,
                additions,
                deletions,
                first,
                last,
                timeline
        );
    }

    public record Detail(
            Metadata metadata,
            Activity activity,
            List<TechnologyEvidence> technologies,
            List<Category> categories,
            Assessment assessment,
            Synchronisation synchronisation
    ) {}

    public record Metadata(
            UUID id,
            String provider,
            String name,
            String fullName,
            String description,
            String htmlUrl,
            String visibility,
            String ownershipRelation,
            String ownerLogin,
            boolean fork,
            boolean archived,
            List<String> topics,
            OffsetDateTime lastActivityAt
    ) {}

    public record Activity(
            int commits,
            int pullRequests,
            int reviews,
            int issues,
            long additions,
            long deletions,
            OffsetDateTime firstActivityAt,
            OffsetDateTime lastActivityAt,
            List<ActivityPoint> timeline
    ) {}

    public record ActivityPoint(String month, int commits) {}

    public record TechnologyEvidence(
            String technologyKey,
            String technologyName,
            String evidenceType,
            String strength,
            String sourceValue,
            Long measuredValue,
            OffsetDateTime observedAt
    ) {}

    public record Category(
            String categoryKey,
            String categoryName,
            String source,
            String confidence,
            Map<String, Object> rationale
    ) {}

    public record Assessment(
            String significanceLevel,
            int significanceScore,
            Map<String, Object> significanceRationale,
            String involvementLevel,
            int involvementScore,
            Map<String, Object> involvementRationale,
            OffsetDateTime calculatedAt
    ) {}

    public record Synchronisation(
            String status,
            OffsetDateTime lastSeenAt,
            String error
    ) {}
}
