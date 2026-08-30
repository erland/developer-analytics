package io.github.developeranalytics.service.technology;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.technology.*;
import io.github.developeranalytics.persistence.technology.RepositoryTechnologyEvidenceRepository;
import io.github.developeranalytics.persistence.technology.TechnologyCatalogueRepository;
import io.github.developeranalytics.persistence.technology.UserTechnologyAssessmentRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class TechnologyEvidenceStrengthService {

    @Inject
    RepositoryTechnologyEvidenceRepository evidence;

    @Inject
    TechnologyCatalogueRepository catalogue;

    @Inject
    UserTechnologyAssessmentRepository assessments;

    @Transactional
    public int recalculate(AppUser user) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime recentThreshold = now.minusMonths(12);

        int updated = 0;

        for (var row : evidence.summarizeForUser(
                user.getId(),
                recentThreshold
        )) {
            TechnologyCatalogueEntry technology = catalogue
                    .findById(row.technologyId())
                    .orElseThrow();

            Score score = calculate(
                    row.repositoryCount(),
                    row.evidenceCount(),
                    row.independentEvidenceTypes(),
                    row.firstObservedAt(),
                    row.lastObservedAt(),
                    row.recentRepositoryCount(),
                    now
            );

            UserTechnologyAssessment assessment = assessments
                    .find(user.getId(), technology.getId())
                    .orElseGet(() -> {
                        UserTechnologyAssessment created =
                                new UserTechnologyAssessment(user, technology);
                        assessments.persist(created);
                        return created;
                    });

            assessment.update(
                    score.strength(),
                    row.repositoryCount(),
                    row.evidenceCount(),
                    row.independentEvidenceTypes(),
                    row.firstObservedAt(),
                    row.lastObservedAt(),
                    row.recentRepositoryCount(),
                    score.score(),
                    score.rationale(),
                    io.github.developeranalytics.domain.model.DataPrivacyProvenance.fromRepositoryCounts(row.publicRepositoryCount(), row.privateRepositoryCount()),
                    now
            );
            updated++;
        }

        return updated;
    }

    public Score calculate(
            int repositoryCount,
            int evidenceCount,
            int independentEvidenceTypes,
            OffsetDateTime firstObservedAt,
            OffsetDateTime lastObservedAt,
            int recentRepositoryCount,
            OffsetDateTime now
    ) {
        int score = 0;
        Map<String, Object> rationale = new LinkedHashMap<>();

        int projectPoints = repositoryCount >= 5 ? 35
                : repositoryCount >= 3 ? 25
                : repositoryCount >= 2 ? 15
                : repositoryCount >= 1 ? 8
                : 0;
        score += projectPoints;
        rationale.put("projectBreadthPoints", projectPoints);

        int evidencePoints = independentEvidenceTypes >= 3 ? 25
                : independentEvidenceTypes == 2 ? 15
                : independentEvidenceTypes == 1 ? 7
                : 0;
        score += evidencePoints;
        rationale.put("independentEvidencePoints", evidencePoints);

        int volumePoints = evidenceCount >= 10 ? 15
                : evidenceCount >= 5 ? 10
                : evidenceCount >= 2 ? 5
                : evidenceCount >= 1 ? 2
                : 0;
        score += volumePoints;
        rationale.put("evidenceVolumePoints", volumePoints);

        int durationPoints = 0;
        if (firstObservedAt != null && lastObservedAt != null) {
            long months = java.time.temporal.ChronoUnit.MONTHS.between(
                    firstObservedAt,
                    lastObservedAt
            );
            durationPoints = months >= 24 ? 15
                    : months >= 12 ? 10
                    : months >= 3 ? 5
                    : 2;
        }
        score += durationPoints;
        rationale.put("durationPoints", durationPoints);

        int recencyPoints = recentRepositoryCount >= 3 ? 10
                : recentRepositoryCount >= 1 ? 6
                : 0;
        score += recencyPoints;
        rationale.put("recencyPoints", recencyPoints);

        score = Math.min(score, 100);

        TechnologyEvidenceStrength strength =
                score >= 70 ? TechnologyEvidenceStrength.STRONG
                : score >= 45 ? TechnologyEvidenceStrength.MODERATE
                : score >= 20 ? TechnologyEvidenceStrength.LIMITED
                : TechnologyEvidenceStrength.EXPOSURE;

        rationale.put("score", score);
        rationale.put("strength", strength.name());
        rationale.put("interpretation",
                "Evidence strength reflects observed open-source activity, not formal proficiency.");

        return new Score(strength, score, rationale);
    }

    public record Score(
            TechnologyEvidenceStrength strength,
            int score,
            Map<String, Object> rationale
    ) {}
}
