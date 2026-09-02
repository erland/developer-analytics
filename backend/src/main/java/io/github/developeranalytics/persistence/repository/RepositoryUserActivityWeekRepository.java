package io.github.developeranalytics.persistence.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class RepositoryUserActivityWeekRepository {
    @Inject EntityManager entityManager;

    public void replace(UUID userId, UUID repositoryId, List<WeekInput> weeks, OffsetDateTime observedAt) {
        entityManager.createNativeQuery("delete from repository_user_activity_week where user_id=:userId and repository_id=:repositoryId")
                .setParameter("userId",userId).setParameter("repositoryId",repositoryId).executeUpdate();
        for(WeekInput week:weeks){
            entityManager.createNativeQuery(
                    "insert into repository_user_activity_week (user_id,repository_id,week_start,commits,additions,deletions,observed_at) " +
                    "values (:userId,:repositoryId,:weekStart,:commits,:additions,:deletions,:observedAt)")
                    .setParameter("userId",userId).setParameter("repositoryId",repositoryId)
                    .setParameter("weekStart",week.weekStart()).setParameter("commits",week.commits())
                    .setParameter("additions",week.additions()).setParameter("deletions",week.deletions())
                    .setParameter("observedAt",observedAt).executeUpdate();
        }
    }

    public List<WeekRow> findForUser(UUID userId){
        List<?> result = entityManager.createNativeQuery(
                "select w.repository_id,w.week_start,w.commits,w.additions,w.deletions " +
                "from repository_user_activity_week w join source_repository r on r.id=w.repository_id " +
                "where w.user_id=:userId and r.user_id=:userId and r.included_in_analysis=true order by w.week_start")
                .setParameter("userId",userId).getResultList();
        return result.stream().map(value -> row((Object[]) value)).toList();
    }

    public List<WeekRow> findForRepository(UUID userId,UUID repositoryId){
        List<?> result = entityManager.createNativeQuery(
                "select repository_id,week_start,commits,additions,deletions from repository_user_activity_week " +
                "where user_id=:userId and repository_id=:repositoryId order by week_start")
                .setParameter("userId",userId).setParameter("repositoryId",repositoryId)
                .getResultList();
        return result.stream().map(value -> row((Object[]) value)).toList();
    }

    private WeekRow row(Object[] row){return new WeekRow((UUID)row[0],toLocalDate(row[1]),((Number)row[2]).intValue(),((Number)row[3]).longValue(),((Number)row[4]).longValue());}
    private LocalDate toLocalDate(Object value){if(value instanceof LocalDate d)return d;if(value instanceof java.sql.Date d)return d.toLocalDate();throw new IllegalStateException("Unsupported week date value: "+value);}

    public record WeekInput(LocalDate weekStart,int commits,long additions,long deletions){}
    public record WeekRow(UUID repositoryId,LocalDate weekStart,int commits,long additions,long deletions){public long changedLines(){return additions+deletions;}}
}
