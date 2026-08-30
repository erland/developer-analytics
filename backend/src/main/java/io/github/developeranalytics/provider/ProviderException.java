package io.github.developeranalytics.provider;

public class ProviderException extends Exception {
    private final int statusCode;

    public ProviderException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public ProviderException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
