package com.osc.backend.common.exception;

/**
 * Exception thrown when a sensitive action requires fresh authentication credentials
 * or re-authentication before execution.
 */
public class ReauthenticationRequiredException extends RuntimeException {

    public ReauthenticationRequiredException(String message) {
        super(message);
    }
}
