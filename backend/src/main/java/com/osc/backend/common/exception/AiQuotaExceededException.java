package com.osc.backend.common.exception;

/**
 * Exception thrown when a user exceeds their daily AI search quota.
 */
public class AiQuotaExceededException extends RuntimeException {

    public AiQuotaExceededException(String message) {
        super(message);
    }
}
