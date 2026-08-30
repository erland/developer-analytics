package io.github.developeranalytics.domain.technology;

import io.github.developeranalytics.domain.model.AppUser;
import io.github.developeranalytics.domain.model.SourceRepository;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RepositoryTechnologyEvidenceTest {

    @Test
    void storesObservedLanguageEvidenceWithoutClaimingExpertise() {
        AppUser user = AppUser.create();
        SourceRepository repository = new SourceRepository(
                user, "github", "repo-1", "alice", "demo");
        TechnologyCatalogueEntry java = new TechnologyCatalogueEntry(
                "java",
                "Java",
                TechnologyCategory.LANGUAGE,
                null,
                null,
                List.of(),
                List.of("Java"),
                List.of(),
                List.of()
        );

        OffsetDateTime observed =
                OffsetDateTime.parse("2026-08-30T08:00:00Z");

        RepositoryTechnologyEvidence evidence =
                new RepositoryTechnologyEvidence(
                        user,
                        repository,
                        java,
                        TechnologyEvidenceType.LANGUAGE,
                        TechnologyEvidenceStrength.OBSERVED,
                        "Java",
                        120000L,
                        observed
                );

        assertEquals(TechnologyEvidenceType.LANGUAGE, evidence.getEvidenceType());
        assertEquals(TechnologyEvidenceStrength.OBSERVED, evidence.getStrength());
        assertEquals("Java", evidence.getSourceValue());
        assertEquals(120000L, evidence.getMeasuredValue());
    }
}
