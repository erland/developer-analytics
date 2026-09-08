package io.github.developeranalytics.domain.change;

import java.util.Objects;

/**
 * Explainable result of classifying one changed file.
 *
 * @param kind change kind assigned to the changed file
 * @param confidence deterministic confidence in the range 0.0-1.0
 * @param ruleKey stable key identifying the rule that produced the result
 * @param classifierVersion version of the classifier/rule set
 */
public record ChangeKindClassification(
        ChangeKind kind,
        double confidence,
        String ruleKey,
        String classifierVersion
) {
    public static final String FALLBACK_RULE_KEY = "fallback-other";

    public ChangeKindClassification {
        Objects.requireNonNull(kind, "kind must not be null");
        ruleKey = requireNonBlank(ruleKey, "ruleKey");
        classifierVersion = requireNonBlank(classifierVersion, "classifierVersion");
        if (!Double.isFinite(confidence) || confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException("confidence must be finite and between 0.0 and 1.0");
        }
    }

    /**
     * Safe result for a file that cannot be classified by the active rule set.
     */
    public static ChangeKindClassification otherFallback(String classifierVersion) {
        return new ChangeKindClassification(
                ChangeKind.OTHER,
                0.0,
                FALLBACK_RULE_KEY,
                classifierVersion
        );
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
