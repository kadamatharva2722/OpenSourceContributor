package com.osc.backend.github.exception;

/**
 * Exception thrown when a requested GitHub resource (repository, issue, user) is not found (HTTP 404).
 */
public class GitHubResourceNotFoundException extends GitHubApiException {

    private final String resource;

    public GitHubResourceNotFoundException(String resource, String message) {
        super(message, 404);
        this.resource = resource;
    }

    public GitHubResourceNotFoundException(String resource, String message, Throwable cause) {
        super(message, 404, cause);
        this.resource = resource;
    }

    public String getResource() {
        return resource;
    }
}
