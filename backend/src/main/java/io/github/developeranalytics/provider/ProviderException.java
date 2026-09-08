package io.github.developeranalytics.provider;

import java.time.OffsetDateTime;

public class ProviderException extends Exception {
    private final int statusCode;
    private final OffsetDateTime retryAt;

    public ProviderException(String message, int statusCode) {
        this(message, statusCode, null, null);
    }

    public ProviderException(String message, int statusCode, Throwable cause) {
        this(message, statusCode, null, cause);
    }

    public ProviderException(String message, int statusCode, OffsetDateTime retryAt) {
        this(message, statusCode, retryAt, null);
    }

    public ProviderException(String message, int statusCode, OffsetDateTime retryAt, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.retryAt = retryAt;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Provider-supplied earliest retry time, when available. Callers should not
     * issue another request before this instant.
     */
    public OffsetDateTime getRetryAt() {
        return retryAt;
    }
}
