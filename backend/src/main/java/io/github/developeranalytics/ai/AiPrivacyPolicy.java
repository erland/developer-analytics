package io.github.developeranalytics.ai;

public enum AiPrivacyPolicy {
    PUBLIC_ONLY,
    PRIVATE_METADATA_ALLOWED,
    PRIVATE_AI_DISABLED;

    public boolean allowsPrivateMetadata() {
        return this == PRIVATE_METADATA_ALLOWED;
    }
}
