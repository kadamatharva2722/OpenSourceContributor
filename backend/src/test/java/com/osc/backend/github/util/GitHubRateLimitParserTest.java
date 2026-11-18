package com.osc.backend.github.util;

import com.osc.backend.github.model.GitHubRateLimit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GitHubRateLimitParser Unit Tests")
class GitHubRateLimitParserTest {

    @Test
    @DisplayName("Parses standard GitHub rate limit headers accurately")
    void parse_ValidHeaders_ReturnsPopulatedRateLimit() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Limit", "60");
        headers.set("X-RateLimit-Remaining", "42");
        headers.set("X-RateLimit-Reset", "1700000000");
        headers.set("Retry-After", "30");

        GitHubRateLimit rateLimit = GitHubRateLimitParser.parse(headers);

        assertNotNull(rateLimit);
        assertEquals(60, rateLimit.getLimit());
        assertEquals(42, rateLimit.getRemaining());
        assertEquals(Instant.ofEpochSecond(1700000000L), rateLimit.getResetTime());
        assertEquals(30L, rateLimit.getRetryAfterSeconds());
        assertFalse(rateLimit.isExhausted());
    }

    @Test
    @DisplayName("Identifies exhausted rate limit when remaining is zero")
    void parse_ZeroRemaining_IdentifiesExhausted() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Limit", "60");
        headers.set("X-RateLimit-Remaining", "0");
        headers.set("X-RateLimit-Reset", "1700000000");

        GitHubRateLimit rateLimit = GitHubRateLimitParser.parse(headers);

        assertNotNull(rateLimit);
        assertEquals(0, rateLimit.getRemaining());
        assertTrue(rateLimit.isExhausted());
    }

    @Test
    @DisplayName("Returns null when no rate limit headers are present")
    void parse_NoHeaders_ReturnsNull() {
        HttpHeaders headers = new HttpHeaders();
        assertNull(GitHubRateLimitParser.parse(headers));
        assertNull(GitHubRateLimitParser.parse(null));
    }

    @Test
    @DisplayName("Handles malformed values gracefully without throwing")
    void parse_MalformedHeaders_HandlesSafely() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-RateLimit-Limit", "not-a-number");
        headers.set("X-RateLimit-Remaining", "xyz");
        headers.set("X-RateLimit-Reset", "invalid-timestamp");

        GitHubRateLimit rateLimit = GitHubRateLimitParser.parse(headers);

        assertNotNull(rateLimit);
        assertEquals(0, rateLimit.getLimit());
        assertEquals(0, rateLimit.getRemaining());
        assertNull(rateLimit.getResetTime());
        assertNull(rateLimit.getRetryAfterSeconds());
    }
}
