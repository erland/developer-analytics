package io.github.developeranalytics.provider.history.git;

import io.github.developeranalytics.domain.change.ChangeKind;
import io.github.developeranalytics.provider.ProviderContributionFileChange;
import io.github.developeranalytics.service.change.ChangeKindClassifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unit")
class GitLocalHistoryReaderTest {

    private final ChangeKindClassifier classifier = new ChangeKindClassifier();

    @Test
    void parsesCodeDocumentationCiAndMixedNumstatWithoutDoubleCounting() {
        String output = String.join("\n",
                "10\t2\tsrc/main/java/App.java",
                "7\t1\tdocs/guide.md",
                "3\t0\t.github/workflows/ci.yml",
                "4\t4\tscripts/release.sh");

        List<ProviderContributionFileChange> changes = GitLocalHistoryReader.parseNumstat(output);

        assertEquals(4, changes.size());
        assertEquals(ChangeKind.CODE, classifier.classify(changes.get(0).path()).kind());
        assertEquals(ChangeKind.DOCUMENTATION, classifier.classify(changes.get(1).path()).kind());
        assertEquals(ChangeKind.CI_CD, classifier.classify(changes.get(2).path()).kind());
        assertEquals(ChangeKind.CODE, classifier.classify(changes.get(3).path()).kind());
        assertEquals(24, changes.stream().mapToInt(ProviderContributionFileChange::additions).sum());
        assertEquals(7, changes.stream().mapToInt(ProviderContributionFileChange::deletions).sum());
    }

    @Test
    void binaryNumstatIsKeptConservativelyWithZeroCounts() {
        List<ProviderContributionFileChange> changes = GitLocalHistoryReader.parseNumstat("-\t-\tassets/logo.png\n");

        assertEquals(1, changes.size());
        assertEquals("assets/logo.png", changes.get(0).path());
        assertEquals(0, changes.get(0).additions());
        assertEquals(0, changes.get(0).deletions());
        assertEquals(ChangeKind.OTHER, classifier.classify(changes.get(0).path()).kind());
    }

    @Test
    void malformedRowsAreIgnoredInsteadOfInventingFileChanges() {
        List<ProviderContributionFileChange> changes = GitLocalHistoryReader.parseNumstat(
                "not-numstat\n1\t2\t\n2\t3\tvalid.java\n");

        assertEquals(1, changes.size());
        assertEquals("valid.java", changes.get(0).path());
    }
}
