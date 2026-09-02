package io.github.developeranalytics.persistence.project;

import io.github.developeranalytics.domain.model.Contribution;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.time.YearMonth;
import java.util.*;

@ApplicationScoped
public class ProjectTypeAnalyticsRepository {
    @Inject EntityManager entityManager;

    public List<CategorySummaryRow> categorySummaries(UUID userId) {
        return entityManager.createQuery(
                "select c.category.categoryKey, c.category.displayName, count(distinct c.repository.id) " +
                "from RepositoryProjectCategory c where c.repository.user.id=:userId and c.repository.includedInAnalysis=true " +
                "group by c.category.categoryKey, c.category.displayName order by count(distinct c.repository.id) desc, c.category.displayName",
                Object[].class).setParameter("userId",userId).getResultList().stream()
                .map(row->new CategorySummaryRow((String)row[0],(String)row[1],((Number)row[2]).intValue())).toList();
    }

    public List<CategoryActivityRow> categoryActivity(UUID userId) {
        Map<Key,MutableActivity> grouped=new LinkedHashMap<>();
        List<Object[]> commitRows=entityManager.createQuery(
                "select c.category.categoryKey, co.occurredAt, co.repository.id " +
                "from RepositoryProjectCategory c, Contribution co " +
                "where c.repository.user.id=:userId and c.repository.includedInAnalysis=true " +
                "and co.user.id=:userId and co.type=:commitType and co.repository.id=c.repository.id order by co.occurredAt",
                Object[].class).setParameter("userId",userId).setParameter("commitType",Contribution.Type.COMMIT).getResultList();
        for(Object[] row:commitRows){
            Key key=new Key((String)row[0],YearMonth.from((java.time.OffsetDateTime)row[1]));
            MutableActivity a=grouped.computeIfAbsent(key,k->new MutableActivity());
            a.commitCount++;a.repositories.add((UUID)row[2]);
        }

        List<Object[]> lineRows=entityManager.createNativeQuery(
                "select pc.category_key, date_trunc('month', w.week_start), " +
                "sum(w.additions + w.deletions), sum(w.commits), count(distinct w.repository_id) " +
                "from repository_user_activity_week w " +
                "join (select distinct repository_id, category_id from repository_project_category) rpc on rpc.repository_id=w.repository_id " +
                "join project_category pc on pc.id=rpc.category_id " +
                "join source_repository r on r.id=w.repository_id " +
                "where w.user_id=:userId and r.user_id=:userId and r.included_in_analysis=true " +
                "group by pc.category_key, date_trunc('month', w.week_start)",Object[].class)
                .setParameter("userId",userId).getResultList();
        for(Object[] row:lineRows){
            Key key=new Key((String)row[0],YearMonth.from(toMonth(row[1])));
            MutableActivity a=grouped.computeIfAbsent(key,k->new MutableActivity());
            a.changedLines+=numberLong(row[2]);a.lineStatisticsCommitCount+=number(row[3]);
        }

        return grouped.entrySet().stream().map(entry->new CategoryActivityRow(
                entry.getKey().categoryKey,entry.getKey().month.toString(),entry.getValue().commitCount,
                entry.getValue().changedLines,entry.getValue().lineStatisticsCommitCount,entry.getValue().repositories.size()))
                .sorted(Comparator.comparing(CategoryActivityRow::categoryKey).thenComparing(CategoryActivityRow::month)).toList();
    }

    public List<RepresentativeProjectRow> representativeProjects(UUID userId,String categoryKey,int limit){
        return entityManager.createQuery(
                "select c.repository.id, c.repository.name, c.repository.htmlUrl, c.repository.visibility, " +
                "c.repository.ownershipRelation, c.repository.lastActivityAt, count(co.id) " +
                "from RepositoryProjectCategory c left join Contribution co on co.repository.id=c.repository.id and co.user.id=:userId " +
                "where c.repository.user.id=:userId and c.repository.includedInAnalysis=true and c.category.categoryKey=:categoryKey " +
                "group by c.repository.id,c.repository.name,c.repository.htmlUrl,c.repository.visibility,c.repository.ownershipRelation,c.repository.lastActivityAt " +
                "order by count(co.id) desc,c.repository.lastActivityAt desc nulls last",Object[].class)
                .setParameter("userId",userId).setParameter("categoryKey",categoryKey)
                .setMaxResults(Math.max(1,Math.min(limit,1000))).getResultList().stream()
                .map(row->new RepresentativeProjectRow((UUID)row[0],(String)row[1],(String)row[2],row[3].toString(),row[4].toString(),
                        (java.time.OffsetDateTime)row[5],((Number)row[6]).intValue())).toList();
    }

    private java.time.LocalDate toMonth(Object value){
        if(value instanceof java.sql.Timestamp v)return v.toLocalDateTime().toLocalDate().withDayOfMonth(1);
        if(value instanceof java.time.LocalDateTime v)return v.toLocalDate().withDayOfMonth(1);
        if(value instanceof java.time.OffsetDateTime v)return v.toLocalDate().withDayOfMonth(1);
        if(value instanceof java.time.LocalDate v)return v.withDayOfMonth(1);
        if(value instanceof java.sql.Date v)return v.toLocalDate().withDayOfMonth(1);
        throw new IllegalStateException("Unsupported month value: "+value);
    }
    private int number(Object value){return value==null?0:((Number)value).intValue();}
    private long numberLong(Object value){return value==null?0L:((Number)value).longValue();}
    private record Key(String categoryKey,YearMonth month){}
    private static class MutableActivity{int commitCount;long changedLines;int lineStatisticsCommitCount;final Set<UUID> repositories=new HashSet<>();}
    public record CategorySummaryRow(String categoryKey,String categoryName,int projectCount){}
    public record CategoryActivityRow(String categoryKey,String month,int commitCount,long changedLines,int lineStatisticsCommitCount,int activeProjectCount){}
    public record RepresentativeProjectRow(UUID repositoryId,String repositoryName,String htmlUrl,String visibility,String ownershipRelation,java.time.OffsetDateTime lastActivityAt,int contributionCount){}
}
