package com.osc.backend.discovery.exception;

/**
 * Exception thrown when the AI service (Groq / Spring AI) is not configured,
 * disabled, or temporarily unavailable.
 */
public class AiServiceUnavailableException extends DiscoveryException {

    public AiServiceUnavailableException(String message) {
        super(message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
