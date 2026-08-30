package io.github.developeranalytics.service.technology;

import io.github.developeranalytics.domain.technology.TechnologyEvidenceStrength;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TechnologyEvidenceStrengthServiceTest {

    private final TechnologyEvidenceStrengthService service =
            new TechnologyEvidenceStrengthService();

    @Test
    void exposureForSingleWeakSignal() {
        OffsetDateTime now =
                OffsetDateTime.parse("2026-08-30T08:00:00Z");

        var result = service.calculate(
                1,
                1,
                1,
                now,
                now,
                0,
                now
        );

        assertEquals(
                TechnologyEvidenceStrength.EXPOSURE,
                result.strength()
        );
    }

    @Test
    void moderateForRepeatedMultiSignalEvidence() {
        OffsetDateTime now =
                OffsetDateTime.parse("2026-08-30T08:00:00Z");

        var result = service.calculate(
                3,
                6,
                2,
                now.minusMonths(14),
                now,
                2,
                now
        );

        assertEquals(
                TechnologyEvidenceStrength.MODERATE,
                result.strength()
        );
    }

    @Test
    void strongRequiresBroadDeepAndRecentEvidence() {
        OffsetDateTime now =
                OffsetDateTime.parse("2026-08-30T08:00:00Z");

        var result = service.calculate(
                6,
                15,
                3,
                now.minusMonths(30),
                now,
                4,
                now
        );

        assertEquals(
                TechnologyEvidenceStrength.STRONG,
                result.strength()
        );
        assertEquals(100, result.score());
    }
}
