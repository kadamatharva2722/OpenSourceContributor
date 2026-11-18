package com.osc.backend.github.exception;

import com.osc.backend.github.model.GitHubRateLimit;

import java.time.Instant;

/**
 * Exception thrown when GitHub API rate limits are exceeded (HTTP 403 with 0 remaining, or HTTP 429).
 */
public class GitHubRateLimitExceededException extends GitHubApiException {

    private final GitHubRateLimit rateLimit;

    public GitHubRateLimitExceededException(String message, GitHubRateLimit rateLimit) {
        super(message, 429);
        this.rateLimit = rateLimit;
    }

    public GitHubRateLimitExceededException(String message, GitHubRateLimit rateLimit, Throwable cause) {
        super(message, 429, cause);
        this.rateLimit = rateLimit;
    }

    public GitHubRateLimit getRateLimit() {
        return rateLimit;
    }

    public Instant getResetTime() {
        return rateLimit != null ? rateLimit.getResetTime() : null;
    }

    public Long getRetryAfterSeconds() {
        return rateLimit != null ? rateLimit.getRetryAfterSeconds() : null;
    }
}
