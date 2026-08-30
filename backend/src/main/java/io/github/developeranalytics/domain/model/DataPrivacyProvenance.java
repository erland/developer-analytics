package io.github.developeranalytics.domain.model;

public enum DataPrivacyProvenance {
    PUBLIC_ONLY,
    INCLUDES_PRIVATE,
    PRIVATE_AGGREGATE;

    public static DataPrivacyProvenance fromVisibility(RepositoryVisibility visibility) {
        return visibility == RepositoryVisibility.PRIVATE ? PRIVATE_AGGREGATE : PUBLIC_ONLY;
    }

    public static DataPrivacyProvenance fromRepositoryCounts(int publicRepositoryCount, int privateRepositoryCount) {
        if (privateRepositoryCount <= 0) return PUBLIC_ONLY;
        if (publicRepositoryCount <= 0) return PRIVATE_AGGREGATE;
        return INCLUDES_PRIVATE;
    }

    public boolean containsPrivateEvidence() { return this != PUBLIC_ONLY; }
}
