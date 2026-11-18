package com.osc.backend.common.exception;

/**
 * Exception thrown when a request violates domain constraints or validation rules.
 */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
