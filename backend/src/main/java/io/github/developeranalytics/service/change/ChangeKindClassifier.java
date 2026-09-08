package io.github.developeranalytics.service.change;

import io.github.developeranalytics.domain.change.ChangeKind;
import io.github.developeranalytics.domain.change.ChangeKindClassification;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;
import java.util.Set;

/**
 * Deterministically classifies a changed repository path from observable path/name evidence.
 *
 * <p>Rules are deliberately conservative. In particular, generic scripts are treated as code
 * rather than CI/CD unless they live in a well-known CI/CD location. Unknown files safely fall
 * back to {@link ChangeKind#OTHER}.</p>
 */
@ApplicationScoped
public class ChangeKindClassifier {

    public static final String CLASSIFIER_VERSION = "1";

    private static final Set<String> DOCUMENTATION_EXTENSIONS = Set.of(
            ".md", ".mdx", ".rst", ".adoc", ".asciidoc", ".tex", ".txt"
    );

    private static final Set<String> CODE_EXTENSIONS = Set.of(
            ".c", ".cc", ".cpp", ".cxx", ".h", ".hh", ".hpp", ".hxx",
            ".cs", ".fs", ".fsx", ".go", ".java", ".kt", ".kts", ".scala",
            ".js", ".jsx", ".mjs", ".cjs", ".ts", ".tsx",
            ".py", ".pyw", ".rb", ".php", ".swift", ".m", ".mm",
            ".rs", ".dart", ".lua", ".pl", ".pm", ".r", ".jl",
            ".sh", ".bash", ".zsh", ".fish", ".ps1", ".bat", ".cmd",
            ".sql", ".groovy", ".clj", ".cljs", ".ex", ".exs", ".erl", ".hrl",
            ".hs", ".lhs", ".ml", ".mli", ".vb", ".vbs", ".sol"
    );

    /**
     * Classifies one changed repository-relative path.
     */
    public ChangeKindClassification classify(String path) {
        if (path == null || path.isBlank()) {
            return ChangeKindClassification.otherFallback(CLASSIFIER_VERSION);
        }

        String normalized = normalize(path);
        String fileName = fileName(normalized);

        if (isCiCd(normalized, fileName)) {
            return classification(ChangeKind.CI_CD, 1.0, "known-ci-cd-path");
        }

        if (isDocumentation(normalized, fileName)) {
            return classification(ChangeKind.DOCUMENTATION, 1.0, "documentation-path-or-extension");
        }

        if (hasExtension(fileName, CODE_EXTENSIONS)) {
            return classification(ChangeKind.CODE, 1.0, "source-code-extension");
        }

        return ChangeKindClassification.otherFallback(CLASSIFIER_VERSION);
    }

    private boolean isCiCd(String path, String fileName) {
        if (path.startsWith(".github/workflows/") || path.startsWith(".github/actions/")) {
            return true;
        }
        if (path.startsWith(".circleci/") || path.startsWith(".gitlab/")) {
            return true;
        }
        return fileName.equals(".gitlab-ci.yml")
                || fileName.equals(".gitlab-ci.yaml")
                || fileName.equals("jenkinsfile")
                || fileName.equals("azure-pipelines.yml")
                || fileName.equals("azure-pipelines.yaml");
    }

    private boolean isDocumentation(String path, String fileName) {
        if (hasExtension(fileName, DOCUMENTATION_EXTENSIONS)) {
            return true;
        }

        return hasDirectorySegment(path, "docs")
                || hasDirectorySegment(path, "documentation")
                || hasDirectorySegment(path, "doc")
                || hasDirectorySegment(path, "chapters")
                || hasDirectorySegment(path, "chapter")
                || hasDirectorySegment(path, "book")
                || hasDirectorySegment(path, "books")
                || hasDirectorySegment(path, "manus")
                || hasDirectorySegment(path, "manuscript")
                || hasDirectorySegment(path, "manuscripts");
    }

    private boolean hasDirectorySegment(String path, String directory) {
        return path.startsWith(directory + "/") || path.contains("/" + directory + "/");
    }

    private boolean hasExtension(String fileName, Set<String> extensions) {
        for (String extension : extensions) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }

    private ChangeKindClassification classification(ChangeKind kind, double confidence, String ruleKey) {
        return new ChangeKindClassification(kind, confidence, ruleKey, CLASSIFIER_VERSION);
    }

    private String normalize(String path) {
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("./")) {
            normalized = normalized.substring(2);
        }
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/");
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private String fileName(String path) {
        int slash = path.lastIndexOf('/');
        return slash < 0 ? path : path.substring(slash + 1);
    }
}
