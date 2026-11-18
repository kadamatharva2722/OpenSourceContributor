package com.osc.backend.common.security;

import com.osc.backend.common.exception.ClientRateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Strictly bounded, thread-safe in-memory sliding-window IP rate limiter
 * specifically protecting public submission endpoints:
 * - POST /api/v1/contact
 * - POST /api/v1/reports/issues
 * - POST /api/v1/requests/repositories
 *
 * <p>Security &amp; Concurrency Invariants:
 * <ul>
 *   <li>Never evicts unexpired buckets. Eviction is strictly restricted to buckets whose
 *       sliding window has fully expired. Evicting unexpired buckets resets quota history and
 *       is strictly prohibited.</li>
 *   <li>Enforces a hard maximum capacity on tracked IPs via lock-free atomic reservations
 *       ({@link AtomicInteger} CAS). {@code buckets.size()} cannot exceed {@code maxTrackedIps}.</li>
 *   <li>When capacity is full and all slots are occupied by unexpired buckets, new IPs are
 *       rejected with HTTP 429 and {@code Retry-After: 60} without allocating buckets or spinning.</li>
 *   <li>Existing tracked IPs continue using their active buckets up to their configured quota.</li>
 *   <li>Handles races between bucket lookup and concurrent expired-bucket removal: if a retrieved
 *       bucket is detached by the pruner before acquisition, the request safely retries against
 *       a live bucket in the map.</li>
 * </ul>
 */
@Component
public class PublicSubmissionRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(PublicSubmissionRateLimiter.class);

    private final int maxAttempts;
    private final long windowMillis;
    private final int maxTrackedIps;

    private final ConcurrentHashMap<String, SubmissionBucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger trackedIpCount = new AtomicInteger(0);
    private final AtomicBoolean isPruning = new AtomicBoolean(false);

    private enum AcquireResult {
        ACQUIRED,
        LIMIT_EXCEEDED,
        DETACHED
    }

    private static class SubmissionBucket {
        private long windowStartEpochMs;
        private int count;
        private long lastAccessEpochMs;
        private boolean detached;

        SubmissionBucket(long now) {
            this.windowStartEpochMs = now;
            this.count = 0;
            this.lastAccessEpochMs = now;
            this.detached = false;
        }

        synchronized AcquireResult tryAcquire(long now, int limit, long windowMillis) {
            if (this.detached) {
                return AcquireResult.DETACHED;
            }
            this.lastAccessEpochMs = now;
            if (now - windowStartEpochMs >= windowMillis) {
                windowStartEpochMs = now;
                count = 0;
            }
            if (count < limit) {
                count++;
                return AcquireResult.ACQUIRED;
            }
            return AcquireResult.LIMIT_EXCEEDED;
        }

        synchronized long getRetryAfterSeconds(long now, long windowMillis) {
            long remainingMs = (windowStartEpochMs + windowMillis) - now;
            return Math.max(1, (remainingMs + 999) / 1000L);
        }

        synchronized boolean isExpired(long now, long windowMillis) {
            return now - windowStartEpochMs >= windowMillis;
        }

        synchronized void markDetached() {
            this.detached = true;
        }

        synchronized boolean isDetached() {
            return this.detached;
        }

        synchronized long getLastAccessEpochMs() {
            return lastAccessEpochMs;
        }
    }

    public PublicSubmissionRateLimiter(
            @Value("${app.security.rate-limit.public-submissions-max-attempts:10}") int maxAttempts,
            @Value("${app.security.rate-limit.public-submissions-window-seconds:600}") long windowSeconds,
            @Value("${app.security.rate-limit.public-submissions-max-tracked-ips:5000}") int maxTrackedIps
    ) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.windowMillis = Math.max(1, windowSeconds) * 1000L;
        this.maxTrackedIps = Math.max(100, maxTrackedIps);
        log.info("Initialized PublicSubmissionRateLimiter: {} submissions per {}s, max tracked IPs: {}",
                this.maxAttempts, windowSeconds, this.maxTrackedIps);
    }

    /**
     * Checks client IP rate limit and consumes one attempt token if available.
     * Enforces a strictly bounded memory cap on tracked IPs via lock-free atomic reservations.
     *
     * @param clientIp validated client IP address
     * @throws ClientRateLimitExceededException if rate limit or capacity limit is exceeded
     */
    public void checkRateLimit(String clientIp) {
        String key = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "unknown";

        while (true) {
            long now = System.currentTimeMillis();
            SubmissionBucket bucket = buckets.get(key);

            if (bucket == null) {
                // New untracked IP: trigger maintenance to prune expired entries
                triggerPruningIfFull(now);

                // Re-check in case another concurrent thread just inserted this key
                bucket = buckets.get(key);
                if (bucket == null) {
                    boolean reserved = tryReserveCapacity();
                    if (!reserved) {
                        log.warn("Public submission rate limiter at maximum capacity ({} tracked IPs). Rejecting new IP: {}",
                                maxTrackedIps, ClientIpResolver.maskIp(key));
                        throw new ClientRateLimitExceededException(
                                "Submission rate limiter is temporarily at full capacity. Please try again in 60 seconds.",
                                60
                        );
                    }

                    try {
                        SubmissionBucket newBucket = new SubmissionBucket(now);
                        SubmissionBucket existing = buckets.putIfAbsent(key, newBucket);
                        if (existing != null) {
                            // Another concurrent thread inserted this exact key simultaneously.
                            // Release our duplicate reservation and use the existing bucket.
                            trackedIpCount.decrementAndGet();
                            bucket = existing;
                        } else {
                            bucket = newBucket;
                        }
                    } catch (Throwable t) {
                        trackedIpCount.decrementAndGet();
                        throw t;
                    }
                }
            }

            AcquireResult result = bucket.tryAcquire(now, maxAttempts, windowMillis);
            if (result == AcquireResult.DETACHED) {
                // The bucket was detached/evicted by the pruner concurrently.
                // Loop back to look up or allocate a live bucket in the map.
                continue;
            }

            if (result == AcquireResult.LIMIT_EXCEEDED) {
                long retryAfter = bucket.getRetryAfterSeconds(now, windowMillis);
                log.warn("Public submission rate limit exceeded for IP: {}. Limit: {} per {}s. Retry after: {}s",
                        ClientIpResolver.maskIp(key), maxAttempts, windowMillis / 1000, retryAfter);
                throw new ClientRateLimitExceededException(
                        "Submission rate limit exceeded. You may submit up to " + maxAttempts +
                                " requests per " + (windowMillis / 60000) + " minutes. Please try again in " +
                                retryAfter + " seconds.",
                        retryAfter
                );
            }

            // Successfully acquired
            return;
        }
    }

    private boolean tryReserveCapacity() {
        int current = trackedIpCount.get();
        while (current < maxTrackedIps) {
            if (trackedIpCount.compareAndSet(current, current + 1)) {
                return true;
            }
            current = trackedIpCount.get();
        }
        return false;
    }

    private void triggerPruningIfFull(long now) {
        if (trackedIpCount.get() >= maxTrackedIps && isPruning.compareAndSet(false, true)) {
            try {
                // Only prune entries whose sliding-window state has expired.
                // Never evict an unexpired bucket, because doing so resets that IP's quota.
                for (Map.Entry<String, SubmissionBucket> entry : buckets.entrySet()) {
                    SubmissionBucket b = entry.getValue();
                    synchronized (b) {
                        if (b.isExpired(now, windowMillis)) {
                            if (buckets.remove(entry.getKey(), b)) {
                                b.markDetached();
                                trackedIpCount.decrementAndGet();
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Error during PublicSubmissionRateLimiter expired bucket pruning: {}", ex.getMessage());
            } finally {
                isPruning.set(false);
            }
        }
    }

    /**
     * Visible for testing and diagnostic telemetry.
     */
    public int getTrackedIpCount() {
        return trackedIpCount.get();
    }

    /**
     * Visible for testing.
     */
    public void reset() {
        buckets.clear();
        trackedIpCount.set(0);
    }
}
