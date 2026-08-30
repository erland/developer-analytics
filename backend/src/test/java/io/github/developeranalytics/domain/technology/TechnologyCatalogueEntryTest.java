package io.github.developeranalytics.domain.technology;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class TechnologyCatalogueEntryTest {

    @Test
    void normalizesStableTechnologyKey() {
        TechnologyCatalogueEntry entry = new TechnologyCatalogueEntry(
                "Spring Boot",
                "Spring Boot",
                TechnologyCategory.FRAMEWORK,
                null,
                null,
                List.of("Spring"),
                List.of(),
                List.of(),
                List.of("spring-boot")
        );

        assertEquals("spring-boot", entry.getTechnologyKey());
        assertEquals(TechnologyCategory.FRAMEWORK, entry.getCategory());
        assertEquals(List.of("Spring"), entry.getAliases());
    }

    @Test
    void rejectsInvalidKey() {
        assertThrows(IllegalArgumentException.class, () ->
                new TechnologyCatalogueEntry(
                        "bad/key",
                        "Bad",
                        TechnologyCategory.OTHER,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of()
                ));
    }
}
