package com.osc.backend.admin.exception;

import com.osc.backend.common.exception.ClientRateLimitExceededException;

/**
 * Thrown when an administrator session or client IP is temporarily locked out due to exceeding
 * the maximum allowed consecutive failed password attempts (5 attempts, 15-minute lockout).
 */
public class AdminLockoutException extends ClientRateLimitExceededException {

    public AdminLockoutException(long retryAfterSeconds) {
        super("Too many failed password attempts. Password verification is temporarily locked for 15 minutes. Please try again later.", retryAfterSeconds);
    }
}
