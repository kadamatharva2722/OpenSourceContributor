package com.osc.backend.github.util;

import com.osc.backend.github.model.GitHubRateLimit;
import org.springframework.http.HttpHeaders;

import java.time.Instant;

/**
 * Utility for parsing GitHub API rate limit response headers.
 */
public final class GitHubRateLimitParser {

    public static final String HEADER_RATE_LIMIT_LIMIT = "X-RateLimit-Limit";
    public static final String HEADER_RATE_LIMIT_REMAINING = "X-RateLimit-Remaining";
    public static final String HEADER_RATE_LIMIT_RESET = "X-RateLimit-Reset";
    public static final String HEADER_RETRY_AFTER = "Retry-After";

    private GitHubRateLimitParser() {
    }

    /**
     * Extracts GitHubRateLimit information from HTTP response headers.
     *
     * @param headers HTTP response headers
     * @return populated GitHubRateLimit or null if no rate limit headers are present
     */
    public static GitHubRateLimit parse(HttpHeaders headers) {
        if (headers == null) {
            return null;
        }

        String limitHeader = headers.getFirst(HEADER_RATE_LIMIT_LIMIT);
        String remainingHeader = headers.getFirst(HEADER_RATE_LIMIT_REMAINING);
        String resetHeader = headers.getFirst(HEADER_RATE_LIMIT_RESET);
        String retryAfterHeader = headers.getFirst(HEADER_RETRY_AFTER);

        if (limitHeader == null && remainingHeader == null && resetHeader == null && retryAfterHeader == null) {
            return null;
        }

        int limit = parseIntOrDefault(limitHeader, 0);
        int remaining = parseIntOrDefault(remainingHeader, 0);
        Instant resetTime = parseEpochSeconds(resetHeader);
        Long retryAfter = parseLongOrNull(retryAfterHeader);

        return new GitHubRateLimit(limit, remaining, resetTime, retryAfter);
    }

    private static int parseIntOrDefault(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private static Long parseLongOrNull(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Instant parseEpochSeconds(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            long epochSeconds = Long.parseLong(value.trim());
            return Instant.ofEpochSecond(epochSeconds);
        } catch (Exception ignored) {
            return null;
        }
    }
}
