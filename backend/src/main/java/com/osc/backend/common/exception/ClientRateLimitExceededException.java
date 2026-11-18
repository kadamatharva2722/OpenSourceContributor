package com.osc.backend.common.exception;

/**
 * Thrown when a client IP exceeds the application-level rate limit for public endpoints.
 */
public class ClientRateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public ClientRateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1, retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
