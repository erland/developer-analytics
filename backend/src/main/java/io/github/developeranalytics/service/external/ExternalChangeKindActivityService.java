package io.github.developeranalytics.service.external;

import io.github.developeranalytics.domain.change.ChangeKind;
import io.github.developeranalytics.domain.external.ExternalClientToken;
import io.github.developeranalytics.domain.model.Contribution;
import io.github.developeranalytics.domain.model.RepositoryVisibility;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.*;

@ApplicationScoped
public class ExternalChangeKindActivityService {

    @Inject EntityManager entityManager;

    @Transactional
    public Result activity(UUID userId, ExternalClientToken.PrivacyScope privacyScope,
                           int months, Set<ChangeKind> changeKinds) {
        int safeMonths = Math.max(1, Math.min(months, 120));
        OffsetDateTime threshold = OffsetDateTime.now().minusMonths(safeMonths);
        String visibilityClause = privacyScope.allowsPrivateAggregates()
                ? "" : " and f.repository.visibility=:publicVisibility ";
        var query = entityManager.createQuery(
                "select f.occurredAt, f.contribution.id, f.repository.id, f.repository.visibility " +
                "from ContributionFileChange f where f.user.id=:userId " +
                "and f.repository.includedInAnalysis=true and f.occurredAt>=:threshold " +
                "and f.changeKind in :changeKinds " + visibilityClause + "order by f.occurredAt",
                Object[].class)
                .setParameter("userId", userId)
                .setParameter("threshold", threshold)
                .setParameter("changeKinds", changeKinds);
        if (!privacyScope.allowsPrivateAggregates()) {
            query.setParameter("publicVisibility", RepositoryVisibility.PUBLIC);
        }

        List<Object[]> rows = query.getResultList();
        Set<UUID> seenContributions = new HashSet<>();
        Set<UUID> activeProjects = new HashSet<>();
        Map<YearMonth, MutableMonth> monthly = new TreeMap<>();
        int publicCommits = 0;
        int privateCommits = 0;

        for (Object[] row : rows) {
            OffsetDateTime occurredAt = (OffsetDateTime) row[0];
            UUID contributionId = (UUID) row[1];
            UUID repositoryId = (UUID) row[2];
            RepositoryVisibility visibility = (RepositoryVisibility) row[3];
            if (!seenContributions.add(contributionId)) continue;
            activeProjects.add(repositoryId);
            if (visibility == RepositoryVisibility.PRIVATE) privateCommits++; else publicCommits++;
            MutableMonth month = monthly.computeIfAbsent(YearMonth.from(occurredAt), ignored -> new MutableMonth());
            month.contributions++;
            month.projects.add(repositoryId);
        }

        Map<String,Integer> byType = new LinkedHashMap<>();
        for (Contribution.Type type : Contribution.Type.values()) byType.put(type.name(), 0);
        byType.put(Contribution.Type.COMMIT.name(), seenContributions.size());

        return new Result(
                seenContributions.size(),
                activeProjects.size(),
                Map.copyOf(byType),
                monthly.entrySet().stream()
                        .map(entry -> new Month(entry.getKey().toString(), entry.getValue().contributions,
                                entry.getValue().projects.size()))
                        .toList(),
                privacyProvenance(publicCommits, privateCommits));
    }

    private String privacyProvenance(int publicCount, int privateCount) {
        if (privateCount == 0) return "PUBLIC_ONLY";
        if (publicCount == 0) return "PRIVATE_AGGREGATE";
        return "INCLUDES_PRIVATE";
    }

    private static final class MutableMonth {
        int contributions;
        final Set<UUID> projects = new HashSet<>();
    }

    public record Result(int contributionCount, int activeProjectCount, Map<String,Integer> contributionTypes,
                         List<Month> monthly, String privacyProvenance) {}
    public record Month(String month, int contributions, int activeProjects) {}
}
