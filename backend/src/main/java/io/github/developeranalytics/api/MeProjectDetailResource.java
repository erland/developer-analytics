package io.github.developeranalytics.api;

import io.github.developeranalytics.auth.AuthenticationService;
import io.github.developeranalytics.auth.CurrentUser;
import io.github.developeranalytics.auth.CurrentUserService;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.domain.project.ProjectSignificanceAssessment;
import io.github.developeranalytics.persistence.project.ProjectSignificanceRepository;
import io.github.developeranalytics.persistence.project.RepositoryProjectCategoryRepository;
import io.github.developeranalytics.persistence.repository.RepositoryUserActivityWeekRepository;
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
    @Inject RepositoryUserActivityWeekRepository weeklyActivity;
    @Inject UserCorrectionService corrections;
    @Inject ProjectSignificanceService significanceService;

    @GET
    @Transactional
    public Detail get(@CookieParam(AuthenticationService.SESSION_COOKIE) String sessionToken,
                      @PathParam("repositoryId") UUID repositoryId) {
        CurrentUser current=currentUserService.requireCurrentUser(sessionToken);
        SourceRepository repository=repositories.findByIdForUser(repositoryId,current.user().getId()).orElseThrow(NotFoundException::new);
        Map<String,TechnologyEvidence> technologyMap=new LinkedHashMap<>();
        for(var item:technologyEvidence.findForRepository(current.user().getId(),repositoryId)){
            technologyMap.putIfAbsent(item.getTechnology().getTechnologyKey(),new TechnologyEvidence(
                    item.getTechnology().getTechnologyKey(),item.getTechnology().getDisplayName(),item.getStrength().name()));
        }
        var categories=categoryAssignments.findForRepository(repositoryId).stream().map(assignment->new Category(
                assignment.getCategory().getCategoryKey(),assignment.getCategory().getDisplayName(),assignment.getSource().name(),
                assignment.getConfidence().name(),assignment.getRationale(),assignment.getPrivacyProvenance().name(),false)).toList();
        ProjectSignificanceAssessment assessment=significance.find(current.user().getId(),repositoryId)
                .orElseGet(()->significanceService.calculateAndStore(current.user(),repository));
        return new Detail(
                new Metadata(repository.getId(),repository.getProvider(),repository.getName(),repository.getFullName(),repository.getDescription(),
                        repository.getHtmlUrl(),repository.getVisibility().name(),repository.getOwnershipRelation().name(),repository.getOwnerLogin(),
                        repository.isFork(),repository.isArchived(),repository.getTopics(),repository.getLastActivityAt(),
                        corrections.isProjectExcludedFromAiProfile(current.user().getId(),repository.getId())),
                loadActivity(current.user().getId(),repository),new ArrayList<>(technologyMap.values()),categories,
                assessment==null?null:new Assessment(assessment.getSignificanceLevel().name(),assessment.getSignificanceScore(),
                        assessment.getSignificanceRationale(),assessment.getInvolvementLevel().name(),assessment.getInvolvementScore(),
                        assessment.getInvolvementRationale(),assessment.getCalculatedAt(),assessment.getPrivacyProvenance().name()),
                new Synchronisation(repository.getSyncStatus().name(),repository.getLastSeenAt(),repository.getSyncError()),
                new Contributors(repository.getContributorCount(),repository.getHumanContributorCount(),repository.getBotContributorCount(),repository.getUserCommitCount()));
    }

    private Activity loadActivity(UUID userId,SourceRepository repository){
        UUID repositoryId=repository.getId();
        List<Object[]> rows=entityManager.createQuery(
                "select c.occurredAt,c.type from Contribution c where c.user.id=:userId and c.repository.id=:repositoryId order by c.occurredAt",
                Object[].class).setParameter("userId",userId).setParameter("repositoryId",repositoryId).getResultList();
        class MonthActivity{int commits;long additions;long deletions;long changedLines;int lineStatisticsCommitCount;}
        Map<YearMonth,MonthActivity> perMonth=new TreeMap<>();
        int commits=0,pullRequests=0,reviews=0,issues=0;OffsetDateTime first=null,last=null;
        for(Object[] row:rows){
            OffsetDateTime at=(OffsetDateTime)row[0];var type=(io.github.developeranalytics.domain.model.Contribution.Type)row[1];
            if(first==null||at.isBefore(first))first=at;if(last==null||at.isAfter(last))last=at;
            switch(type){
                case COMMIT->{commits++;perMonth.computeIfAbsent(YearMonth.from(at),x->new MonthActivity()).commits++;}
                case PULL_REQUEST->pullRequests++;
                case REVIEW->reviews++;
                case ISSUE->issues++;
                default->{}
            }
        }
        for(RepositoryUserActivityWeekRepository.WeekRow week:weeklyActivity.findForRepository(userId,repositoryId)){
            MonthActivity month=perMonth.computeIfAbsent(YearMonth.from(week.weekStart()),x->new MonthActivity());
            month.additions+=week.additions();
            month.deletions+=week.deletions();
            month.changedLines+=week.changedLines();
            month.lineStatisticsCommitCount+=week.commits();
        }
        List<ActivityPoint> timeline=perMonth.entrySet().stream().map(entry->new ActivityPoint(
                entry.getKey().toString(),entry.getValue().commits,entry.getValue().additions,entry.getValue().deletions,
                entry.getValue().changedLines,entry.getValue().lineStatisticsCommitCount)).toList();
        long additions=repository.getUserAdditions()==null?0:repository.getUserAdditions();
        long deletions=repository.getUserDeletions()==null?0:repository.getUserDeletions();
        if(repository.getUserCommitCount()!=null)commits=repository.getUserCommitCount();
        return new Activity(commits,pullRequests,reviews,issues,additions,deletions,first,last,timeline);
    }

    public record Detail(Metadata metadata,Activity activity,List<TechnologyEvidence> technologies,List<Category> categories,Assessment assessment,Synchronisation synchronisation,Contributors contributors){}
    public record Metadata(UUID id,String provider,String name,String fullName,String description,String htmlUrl,String visibility,String ownershipRelation,String ownerLogin,boolean fork,boolean archived,List<String> topics,OffsetDateTime lastActivityAt,boolean excludedFromAiProfile){}
    public record Activity(int commits,int pullRequests,int reviews,int issues,long additions,long deletions,OffsetDateTime firstActivityAt,OffsetDateTime lastActivityAt,List<ActivityPoint> timeline){}
    public record ActivityPoint(String month,int commits,long additions,long deletions,long changedLines,int lineStatisticsCommitCount){}
    public record TechnologyEvidence(String technologyKey,String technologyName,String strength){}
    public record Contributors(Integer total,Integer humans,Integer bots,Integer userCommits){}
    public record Category(String categoryKey,String categoryName,String source,String confidence,Map<String,Object> rationale,String privacyProvenance,boolean rejectedByUser){}
    public record Assessment(String significanceLevel,int significanceScore,Map<String,Object> significanceRationale,String involvementLevel,int involvementScore,Map<String,Object> involvementRationale,OffsetDateTime calculatedAt,String privacyProvenance){}
    public record Synchronisation(String status,OffsetDateTime lastSeenAt,String error){}
}
