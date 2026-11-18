package com.osc.backend.github.exception;

/**
 * Base exception for GitHub API integration errors.
 */
public class GitHubApiException extends RuntimeException {

    private final Integer statusCode;

    public GitHubApiException(String message) {
        super(message);
        this.statusCode = null;
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = null;
    }

    public GitHubApiException(String message, Integer statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public GitHubApiException(String message, Integer statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
