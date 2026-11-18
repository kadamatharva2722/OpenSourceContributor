package com.osc.backend.github.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Encapsulates GitHub API rate limit state extracted from response headers.
 */
public class GitHubRateLimit implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int limit;
    private final int remaining;
    private final Instant resetTime;
    private final Long retryAfterSeconds;

    public GitHubRateLimit(int limit, int remaining, Instant resetTime, Long retryAfterSeconds) {
        this.limit = limit;
        this.remaining = remaining;
        this.resetTime = resetTime;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getLimit() {
        return limit;
    }

    public int getRemaining() {
        return remaining;
    }

    public Instant getResetTime() {
        return resetTime;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public boolean isExhausted() {
        return remaining <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubRateLimit that = (GitHubRateLimit) o;
        return limit == that.limit && remaining == that.remaining &&
                Objects.equals(resetTime, that.resetTime) &&
                Objects.equals(retryAfterSeconds, that.retryAfterSeconds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(limit, remaining, resetTime, retryAfterSeconds);
    }

    @Override
    public String toString() {
        return "GitHubRateLimit{" +
                "limit=" + limit +
                ", remaining=" + remaining +
                ", resetTime=" + resetTime +
                ", retryAfterSeconds=" + retryAfterSeconds +
                '}';
    }
}
