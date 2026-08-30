package io.github.developeranalytics.service.project;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import io.github.developeranalytics.persistence.project.ProjectSignificanceRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ProjectSignificanceServiceTest {

    private final ProjectSignificanceService service =
            new ProjectSignificanceService();

    @Test
    void keepsProjectSignificanceSeparateFromUserInvolvement() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(
                user, "github", "repo-1", "org", "important-project");

        OffsetDateTime now =
                OffsetDateTime.parse("2026-08-30T08:00:00Z");

        var metrics =
                new ProjectSignificanceRepository.ProjectMetrics(
                        3,
                        now.minusMonths(2),
                        now.minusDays(10),
                        3,
                        now.minusYears(4),
                        now.minusDays(2),
                        "ORGANIZATION_OWNED",
                        "ORGANIZATION",
                        false,
                        false,
                        1000,
                        3
                );

        var significance =
                service.calculateSignificance(
                        repository,
                        metrics,
                        now
                );

        var involvement =
                service.calculateInvolvement(
                        metrics,
                        now
                );

        assertTrue(significance.score() > involvement.score());
        assertEquals(
                5,
                involvement.contributionScore()
        );
        assertTrue(
                significance.rationale()
                        .containsKey("ecosystemScore")
        );
        assertTrue(
                involvement.rationale()
                        .containsKey("relativeContribution")
        );
    }

    @Test
    void strongPersonalInvolvementCanExistInSmallProject() {
        OffsetDateTime now =
                OffsetDateTime.parse("2026-08-30T08:00:00Z");

        var metrics =
                new ProjectSignificanceRepository.ProjectMetrics(
                        250,
                        now.minusYears(3),
                        now.minusDays(5),
                        60,
                        now.minusYears(3),
                        now.minusDays(5),
                        "OWNED_BY_USER",
                        "USER",
                        false,
                        false,
                        260,
                        1
                );

        var involvement =
                service.calculateInvolvement(
                        metrics,
                        now
                );

        assertTrue(involvement.score() >= 80);
        assertTrue(
                ((Number) involvement.rationale()
                        .get("relativeContribution"))
                        .doubleValue() > 0.90
        );
    }
}
