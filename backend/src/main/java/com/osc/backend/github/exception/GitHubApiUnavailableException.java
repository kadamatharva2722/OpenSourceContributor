package com.osc.backend.github.exception;

/**
 * Exception thrown when the GitHub API is unavailable (5xx response or network failure after retries exhausted).
 */
public class GitHubApiUnavailableException extends GitHubApiException {

    public GitHubApiUnavailableException(String message) {
        super(message, 503);
    }

    public GitHubApiUnavailableException(String message, Throwable cause) {
        super(message, 503, cause);
    }

    public GitHubApiUnavailableException(String message, Integer statusCode, Throwable cause) {
        super(message, statusCode != null ? statusCode : 503, cause);
    }
}
