package io.github.developeranalytics.service.change;

import io.github.developeranalytics.domain.change.ChangeKind;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("unit")
class ChangeKindAcceptanceScenarioTest {

    private final ChangeKindClassifier classifier = new ChangeKindClassifier();

    @Test
    void markdownBookEditsAreDocumentationNotCode() {
        var paths = List.of(
                "chapters/01-introduction.md",
                "book/appendix.adoc",
                "manus/epilogue.txt"
        );

        assertTrue(paths.stream().allMatch(path ->
                classifier.classify(path).kind() == ChangeKind.DOCUMENTATION));
        assertFalse(paths.stream().anyMatch(path ->
                classifier.classify(path).kind() == ChangeKind.CODE));
    }

    @Test
    void sourceApplicationEditsRemainCode() {
        var paths = List.of(
                "backend/src/main/java/example/App.java",
                "frontend/src/App.tsx",
                "scripts/rebuild.py"
        );

        assertTrue(paths.stream().allMatch(path ->
                classifier.classify(path).kind() == ChangeKind.CODE));
    }

    @Test
    void mixedRepositoryCommitCanContributeToSeveralKindsWithoutCollapsingTheCommit() {
        var kinds = EnumSet.noneOf(ChangeKind.class);
        List.of(
                "src/main/java/example/App.java",
                "docs/architecture.md",
                ".github/workflows/ci.yml"
        ).forEach(path -> kinds.add(classifier.classify(path).kind()));

        assertEquals(Set.of(ChangeKind.CODE, ChangeKind.DOCUMENTATION, ChangeKind.CI_CD), kinds);
    }

    @Test
    void ciCdClassificationIsConservative() {
        assertEquals(ChangeKind.CI_CD, classifier.classify(".github/workflows/verify.yml").kind());
        assertEquals(ChangeKind.CI_CD, classifier.classify("Jenkinsfile").kind());
        assertEquals(ChangeKind.CODE, classifier.classify("scripts/deploy.sh").kind());
        assertEquals(ChangeKind.CODE, classifier.classify("tools/release.py").kind());
    }

    @Test
    void omittedSelectionMeansAllAndCombinedSelectionsAreStable() {
        assertTrue(ChangeKindSelection.isAll(ChangeKindSelection.parse(List.of())));
        assertEquals(
                Set.of(ChangeKind.CODE, ChangeKind.DOCUMENTATION),
                ChangeKindSelection.parse(List.of("CODE,DOCUMENTATION"))
        );
    }
}
