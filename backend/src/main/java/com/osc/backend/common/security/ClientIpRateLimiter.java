package com.osc.backend.common.security;

import com.osc.backend.common.exception.ClientRateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded, thread-safe in-memory sliding-window IP rate limiter.
 * Protects unauthenticated guest discovery endpoints from automated scraping and abuse.
 * Completely distinct and decoupled from upstream GitHub REST API rate limits.
 */
@Component
public class ClientIpRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ClientIpRateLimiter.class);

    private final int maxRequestsPerWindow;
    private final long windowMillis;
    private final int maxTrackedIps;

    private final ConcurrentHashMap<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    public record RateLimitInfo(int limit, int remaining, long resetEpochSeconds) {}

    private static class RateLimitBucket {
        private long windowStartEpochMs;
        private int count;
        private long lastAccessEpochMs;

        RateLimitBucket(long now) {
            this.windowStartEpochMs = now;
            this.count = 0;
            this.lastAccessEpochMs = now;
        }

        synchronized boolean tryAcquire(long now, int limit, long windowMillis) {
            this.lastAccessEpochMs = now;
            if (now - windowStartEpochMs >= windowMillis) {
                windowStartEpochMs = now;
                count = 0;
            }
            if (count < limit) {
                count++;
                return true;
            }
            return false;
        }

        synchronized int getRemaining(long now, int limit, long windowMillis) {
            if (now - windowStartEpochMs >= windowMillis) {
                return limit;
            }
            return Math.max(0, limit - count);
        }

        synchronized long getResetEpochSeconds(long windowMillis) {
            return (windowStartEpochMs + windowMillis) / 1000L;
        }

        synchronized long getRetryAfterSeconds(long now, long windowMillis) {
            long remainingMs = (windowStartEpochMs + windowMillis) - now;
            return Math.max(1, (remainingMs + 999) / 1000L);
        }

        synchronized boolean isExpired(long now, long windowMillis) {
            return now - windowStartEpochMs >= windowMillis;
        }

        synchronized long getLastAccessEpochMs() {
            return lastAccessEpochMs;
        }
    }

    public ClientIpRateLimiter() {
        this(60, 3600, 10000);
    }

    public ClientIpRateLimiter(
            @Value("${app.security.rate-limit.guest-requests-per-hour:60}") int maxRequestsPerWindow,
            @Value("${app.security.rate-limit.guest-window-seconds:3600}") long windowSeconds,
            @Value("${app.security.rate-limit.max-tracked-ips:10000}") int maxTrackedIps
    ) {
        this.maxRequestsPerWindow = Math.max(1, maxRequestsPerWindow);
        this.windowMillis = Math.max(1, windowSeconds) * 1000L;
        this.maxTrackedIps = Math.max(100, maxTrackedIps);
        log.info("Initialized ClientIpRateLimiter: {} req per {}s, max tracked IPs: {}",
                this.maxRequestsPerWindow, windowSeconds, this.maxTrackedIps);
    }

    /**
     * Checks client IP rate limit and consumes one request token if available.
     *
     * @param clientIp validated client IP address
     * @throws ClientRateLimitExceededException if rate limit is exceeded
     */
    public void checkRateLimit(String clientIp) {
        String key = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "unknown";
        long now = System.currentTimeMillis();

        ensureCapacity(now);

        RateLimitBucket bucket = buckets.computeIfAbsent(key, k -> new RateLimitBucket(now));
        boolean acquired = bucket.tryAcquire(now, maxRequestsPerWindow, windowMillis);

        if (!acquired) {
            long retryAfter = bucket.getRetryAfterSeconds(now, windowMillis);
            log.warn("Rate limit exceeded for IP: {}. Limit: {} per {}s. Retry after: {}s",
                    key, maxRequestsPerWindow, windowMillis / 1000, retryAfter);
            throw new ClientRateLimitExceededException(
                    "Public discovery rate limit exceeded. Maximum " + maxRequestsPerWindow +
                            " requests per hour. Please authenticate with GitHub for higher limits or try again later.",
                    retryAfter
            );
        }
    }

    /**
     * Retrieves current rate limit telemetry for response headers without consuming quota.
     *
     * @param clientIp client IP address
     * @return RateLimitInfo with limit, remaining, and reset timestamp
     */
    public RateLimitInfo getRateLimitInfo(String clientIp) {
        String key = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "unknown";
        long now = System.currentTimeMillis();

        RateLimitBucket bucket = buckets.get(key);
        if (bucket == null) {
            return new RateLimitInfo(maxRequestsPerWindow, maxRequestsPerWindow, (now + windowMillis) / 1000L);
        }

        int remaining = bucket.getRemaining(now, maxRequestsPerWindow, windowMillis);
        long resetSeconds = bucket.getResetEpochSeconds(windowMillis);
        return new RateLimitInfo(maxRequestsPerWindow, remaining, resetSeconds);
    }

    /**
     * Protects heap memory against exhaustion attacks by enforcing maxTrackedIps bounds.
     */
    private void ensureCapacity(long now) {
        if (buckets.size() >= maxTrackedIps) {
            // Pass 1: Prune expired entries
            buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowMillis));

            // Pass 2: If still full, prune the least-recently accessed entry to protect memory
            if (buckets.size() >= maxTrackedIps) {
                String oldestKey = null;
                long oldestTime = Long.MAX_VALUE;
                for (Map.Entry<String, RateLimitBucket> entry : buckets.entrySet()) {
                    long accessTime = entry.getValue().getLastAccessEpochMs();
                    if (accessTime < oldestTime) {
                        oldestTime = accessTime;
                        oldestKey = entry.getKey();
                    }
                }
                if (oldestKey != null) {
                    buckets.remove(oldestKey);
                }
            }
        }
    }

    /**
     * Visible for testing.
     */
    public int getTrackedIpCount() {
        return buckets.size();
    }
}
