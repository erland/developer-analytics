package io.github.developeranalytics.domain.change;

/**
 * Describes the kind of work represented by a changed file.
 *
 * <p>The value applies to an individual changed-file contribution. It does not
 * classify an entire repository or commit. A single commit may therefore touch
 * several change kinds.</p>
 */
public enum ChangeKind {
    CODE,
    DOCUMENTATION,
    CI_CD,
    OTHER
}
