package io.github.developeranalytics.service.project;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.domain.project.ProjectSignificanceAssessment;
import io.github.developeranalytics.persistence.project.ProjectSignificanceRepository;
import io.github.developeranalytics.persistence.repository.SourceRepositoryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class ProjectSignificanceService {

    @Inject
    SourceRepositoryRepository repositories;

    @Inject
    ProjectSignificanceRepository assessments;

    @Transactional
    public int recalculate(AppUser user) {
        List<SourceRepository> repos =
                repositories.findByUser(user.getId());

        int updated = 0;

        for (SourceRepository repository : repos) {
            calculateAndStore(user, repository);
            updated++;
        }

        return updated;
    }

    @Transactional
    public ProjectSignificanceAssessment calculateAndStore(
            AppUser user,
            SourceRepository repository
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        var metrics = assessments.metrics(
                user.getId(),
                repository.getId()
        );

        Significance significance =
                calculateSignificance(repository, metrics, now);
        Involvement involvement =
                calculateInvolvement(metrics, now);

        ProjectSignificanceAssessment assessment =
                assessments.find(user.getId(), repository.getId())
                    .orElseGet(() -> {
                        ProjectSignificanceAssessment created =
                                new ProjectSignificanceAssessment(
                                        user,
                                        repository
                                );
                        assessments.persist(created);
                        return created;
                    });

        assessment.update(
                level(significance.score()),
                significance.score(),
                significance.popularityScore(),
                significance.contributorScore(),
                significance.longevityScore(),
                significance.ecosystemScore(),
                significance.activityScore(),
                significance.rationale(),
                level(involvement.score()),
                involvement.score(),
                involvement.contributionScore(),
                involvement.durationScore(),
                involvement.recencyScore(),
                involvement.relativeContributionScore(),
                involvement.rationale(),
                now
        );

        return assessment;
    }

    public Significance calculateSignificance(
            SourceRepository repository,
            ProjectSignificanceRepository.ProjectMetrics metrics,
            OffsetDateTime now
    ) {
        int popularity = 0;
        // Until stars/watchers are collected, popularity uses observable
        // repository breadth proxies only and says so explicitly.
        if (!repository.isFork()) popularity += 8;
        if (!repository.isArchived()) popularity += 4;
        popularity = Math.min(popularity, 20);

        int contributors = metrics.totalObservedContributionCount() >= 500 ? 20
                : metrics.totalObservedContributionCount() >= 100 ? 15
                : metrics.totalObservedContributionCount() >= 25 ? 10
                : metrics.totalObservedContributionCount() >= 5 ? 5
                : 1;

        int longevity = durationPoints(
                metrics.discoveredAt(),
                metrics.lastActivityAt(),
                20
        );

        int ecosystem = 0;
        if ("ORGANIZATION".equals(metrics.ownerType())) ecosystem += 10;
        if ("ORGANIZATION_OWNED".equals(metrics.ownershipRelation())) ecosystem += 5;
        if (metrics.categoryCount() >= 3) ecosystem += 5;
        ecosystem = Math.min(ecosystem, 20);

        int activity = recencyScore(metrics.lastActivityAt(), now, 20);

        int score = Math.min(
                100,
                popularity + contributors + longevity + ecosystem + activity
        );

        Map<String, Object> rationale = new LinkedHashMap<>();
        rationale.put("popularityScore", popularity);
        rationale.put(
                "popularityBasis",
                "Current V1 proxy based on fork/archive state until explicit star/watch data is collected."
        );
        rationale.put("contributorScore", contributors);
        rationale.put(
                "contributorBasis",
                "Observed contribution volume in the analysed repository."
        );
        rationale.put("longevityScore", longevity);
        rationale.put("ecosystemScore", ecosystem);
        rationale.put("activityScore", activity);
        rationale.put("score", score);

        return new Significance(
                score,
                popularity,
                contributors,
                longevity,
                ecosystem,
                activity,
                rationale
        );
    }

    public Involvement calculateInvolvement(
            ProjectSignificanceRepository.ProjectMetrics metrics,
            OffsetDateTime now
    ) {
        int contribution = metrics.userContributionCount() >= 200 ? 35
                : metrics.userContributionCount() >= 75 ? 28
                : metrics.userContributionCount() >= 25 ? 20
                : metrics.userContributionCount() >= 5 ? 12
                : metrics.userContributionCount() >= 1 ? 5
                : 0;

        int duration = durationPoints(
                metrics.firstUserContributionAt(),
                metrics.lastUserContributionAt(),
                25
        );

        int recency = recencyScore(
                metrics.lastUserContributionAt(),
                now,
                20
        );

        double relative = metrics.totalObservedContributionCount() == 0
                ? 0.0
                : (double) metrics.userContributionCount()
                    / (double) metrics.totalObservedContributionCount();

        int relativeContribution = relative >= 0.75 ? 20
                : relative >= 0.50 ? 16
                : relative >= 0.25 ? 12
                : relative >= 0.10 ? 8
                : relative > 0 ? 4
                : 0;

        int score = Math.min(
                100,
                contribution + duration + recency + relativeContribution
        );

        Map<String, Object> rationale = new LinkedHashMap<>();
        rationale.put("contributionScore", contribution);
        rationale.put("durationScore", duration);
        rationale.put("recencyScore", recency);
        rationale.put("relativeContributionScore", relativeContribution);
        rationale.put("userContributionCount", metrics.userContributionCount());
        rationale.put(
                "totalObservedContributionCount",
                metrics.totalObservedContributionCount()
        );
        rationale.put("relativeContribution", relative);
        rationale.put("score", score);

        return new Involvement(
                score,
                contribution,
                duration,
                recency,
                relativeContribution,
                rationale
        );
    }

    private int durationPoints(
            OffsetDateTime first,
            OffsetDateTime last,
            int max
    ) {
        if (first == null || last == null) return 0;

        long months = Math.max(
                0,
                ChronoUnit.MONTHS.between(first, last)
        );

        int score = months >= 36 ? max
                : months >= 24 ? (int) Math.round(max * 0.8)
                : months >= 12 ? (int) Math.round(max * 0.6)
                : months >= 3 ? (int) Math.round(max * 0.35)
                : (int) Math.round(max * 0.15);

        return Math.min(max, score);
    }

    private int recencyScore(
            OffsetDateTime last,
            OffsetDateTime now,
            int max
    ) {
        if (last == null) return 0;

        long days = Math.max(
                0,
                ChronoUnit.DAYS.between(last, now)
        );

        return days <= 30 ? max
                : days <= 90 ? (int) Math.round(max * 0.8)
                : days <= 365 ? (int) Math.round(max * 0.55)
                : days <= 730 ? (int) Math.round(max * 0.25)
                : 0;
    }

    private ProjectSignificanceAssessment.Level level(int score) {
        return score >= 75
                ? ProjectSignificanceAssessment.Level.VERY_HIGH
                : score >= 55
                    ? ProjectSignificanceAssessment.Level.HIGH
                    : score >= 30
                        ? ProjectSignificanceAssessment.Level.MEDIUM
                        : ProjectSignificanceAssessment.Level.LOW;
    }

    public record Significance(
            int score,
            int popularityScore,
            int contributorScore,
            int longevityScore,
            int ecosystemScore,
            int activityScore,
            Map<String, Object> rationale
    ) {}

    public record Involvement(
            int score,
            int contributionScore,
            int durationScore,
            int recencyScore,
            int relativeContributionScore,
            Map<String, Object> rationale
    ) {}
}
