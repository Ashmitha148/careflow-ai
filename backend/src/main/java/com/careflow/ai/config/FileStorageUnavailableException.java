package com.careflow.ai.config;

/**
 * Thrown when Cloudinary (file storage) is unavailable — circuit open, retries
 * exhausted, or timed out. Callers map this to a graceful response (the file
 * operation can be retried later) rather than a 500.
 */
public class FileStorageUnavailableException extends RuntimeException {

    public FileStorageUnavailableException(String message) {
        super(message);
    }

    public FileStorageUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}