package io.github.developeranalytics.service.change;

import io.github.developeranalytics.domain.change.ChangeKind;
import io.github.developeranalytics.domain.change.ChangeKindClassification;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("unit")
class ChangeKindClassifierTest {

    private final ChangeKindClassifier classifier = new ChangeKindClassifier();

    @Test
    void classifiesCommonDocumentationExtensions() {
        assertKind(ChangeKind.DOCUMENTATION, "README.md");
        assertKind(ChangeKind.DOCUMENTATION, "chapters/01-introduction.adoc");
        assertKind(ChangeKind.DOCUMENTATION, "novel/manuscript/chapter-12.txt");
        assertKind(ChangeKind.DOCUMENTATION, "book/main.tex");
    }

    @Test
    void documentationDirectoryTakesPrecedenceOverCodeExtension() {
        ChangeKindClassification result = classifier.classify("docs/examples/Example.java");

        assertEquals(ChangeKind.DOCUMENTATION, result.kind());
        assertEquals("documentation-directory", result.ruleKey());
    }

    @Test
    void knownCiCdLocationsTakeHighestPrecedence() {
        assertKind(ChangeKind.CI_CD, ".github/workflows/ci.yml");
        assertKind(ChangeKind.CI_CD, ".github/actions/release/action.yml");
        assertKind(ChangeKind.CI_CD, ".circleci/config.yml");
        assertKind(ChangeKind.CI_CD, "Jenkinsfile");
        assertKind(ChangeKind.CI_CD, "azure-pipelines.yaml");
        assertKind(ChangeKind.CI_CD, ".github/actions/custom/README.md");
    }

    @Test
    void genericScriptsAreCodeNotCiCd() {
        assertKind(ChangeKind.CODE, "scripts/release.sh");
        assertKind(ChangeKind.CODE, "tools/deploy.py");
        assertKind(ChangeKind.CODE, "build/release.ps1");
    }

    @Test
    void classifiesCommonSourceCodeExtensions() {
        assertKind(ChangeKind.CODE, "backend/src/main/java/example/App.java");
        assertKind(ChangeKind.CODE, "frontend/src/App.tsx");
        assertKind(ChangeKind.CODE, "service/main.go");
        assertKind(ChangeKind.CODE, "src/lib.rs");
    }

    @Test
    void leavesConfigurationAndUnknownFilesAsOther() {
        assertKind(ChangeKind.OTHER, "pom.xml");
        assertKind(ChangeKind.OTHER, "package.json");
        assertKind(ChangeKind.OTHER, "docker-compose.yml");
        assertKind(ChangeKind.OTHER, "assets/logo.png");
        assertKind(ChangeKind.OTHER, "Makefile");
    }

    @Test
    void normalizesCaseAndWindowsSeparators() {
        assertKind(ChangeKind.CI_CD, ".GITHUB\\WORKFLOWS\\BUILD.YML");
        assertKind(ChangeKind.DOCUMENTATION, "DOCS\\GUIDE.MDX");
        assertKind(ChangeKind.CODE, "SRC\\MAIN.JAVA");
    }

    @Test
    void safelyFallsBackForMissingPath() {
        ChangeKindClassification nullResult = classifier.classify(null);
        ChangeKindClassification blankResult = classifier.classify("   ");

        assertEquals(ChangeKind.OTHER, nullResult.kind());
        assertEquals(ChangeKindClassification.FALLBACK_RULE_KEY, nullResult.ruleKey());
        assertEquals(ChangeKind.OTHER, blankResult.kind());
        assertEquals(ChangeKindClassifier.CLASSIFIER_VERSION, blankResult.classifierVersion());
    }

    private void assertKind(ChangeKind expected, String path) {
        ChangeKindClassification result = classifier.classify(path);
        assertEquals(expected, result.kind(), () -> path + " should be " + expected);
        assertEquals(ChangeKindClassifier.CLASSIFIER_VERSION, result.classifierVersion());
    }
}
