package com.osc.backend.discovery.exception;

/**
 * Base exception for AI discovery engine operations.
 */
public class DiscoveryException extends RuntimeException {

    public DiscoveryException(String message) {
        super(message);
    }

    public DiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
