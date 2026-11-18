package com.osc.backend.common.exception;

/**
 * Exception thrown when an unauthenticated request attempts to access protected resources.
 */
public class UnauthenticatedException extends RuntimeException {

    public UnauthenticatedException(String message) {
        super(message);
    }
}
