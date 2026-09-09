package io.github.developeranalytics.service.account;

/** Raised when a transactional user-data deletion could not be completed safely. */
public class DataDeletionFailedException extends RuntimeException {

    public DataDeletionFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
