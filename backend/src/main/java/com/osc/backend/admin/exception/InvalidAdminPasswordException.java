package com.osc.backend.admin.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Thrown when an administrator password verification attempt fails due to an incorrect password.
 * Handled as HTTP 401 Unauthorized by GlobalExceptionHandler.
 */
public class InvalidAdminPasswordException extends AuthenticationException {

    public InvalidAdminPasswordException(String message) {
        super(message);
    }
}
