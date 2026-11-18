package com.osc.backend.github.exception;

/**
 * Exception thrown when the GitHub API returns an unexpected or unparseable response payload.
 */
public class GitHubInvalidResponseException extends GitHubApiException {

    public GitHubInvalidResponseException(String message) {
        super(message, 502);
    }

    public GitHubInvalidResponseException(String message, Throwable cause) {
        super(message, 502, cause);
    }
}
