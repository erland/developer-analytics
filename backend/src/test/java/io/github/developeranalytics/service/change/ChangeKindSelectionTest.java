package io.github.developeranalytics.service.change;

import io.github.developeranalytics.domain.change.ChangeKind;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class ChangeKindSelectionTest {

    @Test
    void emptySelectionMeansAllActivity() {
        assertTrue(ChangeKindSelection.isAll(ChangeKindSelection.parse(List.of())));
        assertTrue(ChangeKindSelection.isAll(ChangeKindSelection.parse(null)));
    }

    @Test
    void supportsCommaSeparatedAndRepeatedValues() {
        Set<ChangeKind> kinds = ChangeKindSelection.parse(List.of("CODE,DOCUMENTATION", "ci-cd"));

        assertEquals(Set.of(ChangeKind.CODE, ChangeKind.DOCUMENTATION, ChangeKind.CI_CD), kinds);
    }

    @Test
    void normalizesCaseAndWhitespace() {
        assertEquals(Set.of(ChangeKind.CODE, ChangeKind.OTHER),
                ChangeKindSelection.parse(List.of(" code ", "other")));
    }

    @Test
    void rejectsUnknownKindsInsteadOfSilentlyIgnoringThem() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> ChangeKindSelection.parse(List.of("CODE,NOT_A_KIND")));
        assertTrue(error.getMessage().contains("NOT_A_KIND"));
    }
}
