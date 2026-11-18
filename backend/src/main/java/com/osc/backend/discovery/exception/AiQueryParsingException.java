package com.osc.backend.discovery.exception;

/**
 * Exception thrown when the AI query parser produces an invalid or unparseable response.
 */
public class AiQueryParsingException extends DiscoveryException {

    public AiQueryParsingException(String message) {
        super(message);
    }

    public AiQueryParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
