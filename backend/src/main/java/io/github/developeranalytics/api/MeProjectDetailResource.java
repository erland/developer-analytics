package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.domain.project.ProjectSignificanceAssessment;
import io.github.developeranalytics.persistence.project.ProjectSignificanceRepository;
import io.github.developeranalytics.persistence.project.RepositoryProjectCategoryRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import io.github.developeranalytics.persistence.technology.RepositoryTechnologyEvidenceRepository;
import io.github.developeranalytics.service.correction.UserCorrectionService;
import io.github.developeranalytics.service.project.ProjectSignificanceService;
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
    @Inject UserCorrectionService corrections;
    @Inject ProjectSignificanceService significanceService;

    @GET
    @Transactional
    public Detail get(@CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
                      @PathParam("repositoryId") UUID repositoryId) {
        CurrentUser current = currentUserService.requireCurrentUser(sessionToken);
        SourceRepository repository = repositories.findByIdForUser(repositoryId, current.user().getId())
                .orElseThrow(NotFoundException::new);

        Map<String, TechnologyEvidence> technologyMap = new LinkedHashMap<>();
        for (var evidenceItem : technologyEvidence.findForRepository(current.user().getId(), repositoryId)) {
            technologyMap.putIfAbsent(evidenceItem.getTechnology().getTechnologyKey(), new TechnologyEvidence(
                    evidenceItem.getTechnology().getTechnologyKey(), evidenceItem.getTechnology().getDisplayName(),
                    evidenceItem.getStrength().name()));
        }
        var technologies = new ArrayList<>(technologyMap.values());

        var categories = categoryAssignments.findForRepository(repositoryId).stream()
                .map(assignment -> new Category(
                        assignment.getCategory().getCategoryKey(), assignment.getCategory().getDisplayName(),
                        assignment.getSource().name(), assignment.getConfidence().name(), assignment.getRationale(),
                        assignment.getPrivacyProvenance().name(),
                        corrections.isProjectCategoryRejected(current.user().getId(), repository.getId(),
                                assignment.getCategory().getCategoryKey())))
                .toList();

        ProjectSignificanceAssessment assessment = significance.find(current.user().getId(), repositoryId)
                .orElseGet(() -> significanceService.calculateAndStore(current.user(), repository));
        var activity = loadActivity(current.user().getId(), repository);

        return new Detail(
                new Metadata(repository.getId(), repository.getProvider(), repository.getName(), repository.getFullName(),
                        repository.getDescription(), repository.getHtmlUrl(), repository.getVisibility().name(),
                        repository.getOwnershipRelation().name(), repository.getOwnerLogin(), repository.isFork(),
                        repository.isArchived(), repository.getTopics(), repository.getLastActivityAt(),
                        corrections.isProjectExcludedFromAiProfile(current.user().getId(), repository.getId())),
                activity, technologies, categories,
                assessment == null ? null : new Assessment(
                        assessment.getSignificanceLevel().name(), assessment.getSignificanceScore(),
                        assessment.getSignificanceRationale(), assessment.getInvolvementLevel().name(),
                        assessment.getInvolvementScore(), assessment.getInvolvementRationale(),
                        assessment.getCalculatedAt(), assessment.getPrivacyProvenance().name()),
                new Synchronisation(repository.getSyncStatus().name(), repository.getLastSeenAt(), repository.getSyncError()),
                new Contributors(repository.getContributorCount(), repository.getHumanContributorCount(),
                        repository.getBotContributorCount(), repository.getUserCommitCount())
        );
    }

    private Activity loadActivity(UUID userId, SourceRepository repository) {
        UUID repositoryId = repository.getId();
        List<Object[]> rows = entityManager.createQuery(
                "select c.occurredAt, c.type, c.additions, c.deletions from Contribution c " +
                "where c.user.id=:userId and c.repository.id=:repositoryId order by c.occurredAt",
                Object[].class)
            .setParameter("userId", userId)
            .setParameter("repositoryId", repositoryId)
            .getResultList();

        class MonthActivity {
            int commits;
            long changedLines;
            int lineStatisticsCommitCount;
        }
        Map<YearMonth, MonthActivity> activityPerMonth = new TreeMap<>();
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
                    MonthActivity month = activityPerMonth.computeIfAbsent(YearMonth.from(occurredAt), ignored -> new MonthActivity());
                    month.commits++;
                    if (add != null || del != null) {
                        month.changedLines += (add == null ? 0 : add) + (del == null ? 0 : del);
                        month.lineStatisticsCommitCount++;
                    }
                    additions += add == null ? 0 : add;
                    deletions += del == null ? 0 : del;
                }
                case PULL_REQUEST -> pullRequests++;
                case REVIEW -> reviews++;
                case ISSUE -> issues++;
                default -> { }
            }
        }

        List<ActivityPoint> timeline = activityPerMonth.entrySet().stream()
                .map(entry -> new ActivityPoint(entry.getKey().toString(), entry.getValue().commits,
                        entry.getValue().changedLines, entry.getValue().lineStatisticsCommitCount))
                .toList();

        if (repository.getUserAdditions() != null) additions = repository.getUserAdditions();
        if (repository.getUserDeletions() != null) deletions = repository.getUserDeletions();
        if (repository.getUserCommitCount() != null) commits = repository.getUserCommitCount();

        return new Activity(commits, pullRequests, reviews, issues, additions, deletions, first, last, timeline);
    }

    public record Detail(Metadata metadata, Activity activity, List<TechnologyEvidence> technologies,
                         List<Category> categories, Assessment assessment, Synchronisation synchronisation,
                         Contributors contributors) {}

    public record Metadata(UUID id, String provider, String name, String fullName, String description, String htmlUrl,
                           String visibility, String ownershipRelation, String ownerLogin, boolean fork, boolean archived,
                           List<String> topics, OffsetDateTime lastActivityAt, boolean excludedFromAiProfile) {}

    public record Activity(int commits, int pullRequests, int reviews, int issues, long additions, long deletions,
                           OffsetDateTime firstActivityAt, OffsetDateTime lastActivityAt, List<ActivityPoint> timeline) {}

    public record ActivityPoint(String month, int commits, long changedLines, int lineStatisticsCommitCount) {}
    public record TechnologyEvidence(String technologyKey, String technologyName, String strength) {}
    public record Contributors(Integer total, Integer humans, Integer bots, Integer userCommits) {}
    public record Category(String categoryKey, String categoryName, String source, String confidence,
                           Map<String, Object> rationale, String privacyProvenance, boolean rejectedByUser) {}
    public record Assessment(String significanceLevel, int significanceScore, Map<String, Object> significanceRationale,
                             String involvementLevel, int involvementScore, Map<String, Object> involvementRationale,
                             OffsetDateTime calculatedAt, String privacyProvenance) {}
    public record Synchronisation(String status, OffsetDateTime lastSeenAt, String error) {}
}
