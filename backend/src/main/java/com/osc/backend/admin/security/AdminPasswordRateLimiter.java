package com.osc.backend.admin.security;

import com.osc.backend.common.security.ClientIpResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Server-side attempt tracker and rate limiter for administrator password verification.
 *
 * <p>Enforces two layers of protection:
 * <ol>
 *   <li><strong>Session-level tracking:</strong> Up to 5 failed password attempts per authenticated
 *       admin session. Exceeding 5 failures locks the session for 15 minutes.</li>
 *   <li><strong>Client IP tracking:</strong> Up to 5 failed attempts per client IP within a
 *       15-minute sliding window, guarding against attackers who attempt to bypass session
 *       limits by cycling OAuth logout and login.</li>
 * </ol>
 *
 * <p>Memory is strictly bounded to {@value #MAX_TRACKED_IPS} client IPs using atomic CAS reservations.
 * Only expired entries are pruned; unexpired entries are never evicted to prevent quota reset or lockout bypass.
 * If capacity is reached, the limiter fails closed for new IPs without resetting existing lockouts.
 */
@Component
public class AdminPasswordRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(AdminPasswordRateLimiter.class);

    public static final String SESSION_ATTR_FAILED_ATTEMPTS = "OSC_ADMIN_FAILED_PASSWORD_ATTEMPTS";
    public static final String SESSION_ATTR_LOCKOUT_UNTIL = "OSC_ADMIN_PASSWORD_LOCKOUT_UNTIL";

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_MS = 15 * 60 * 1000L; // 15 minutes
    public static final int MAX_TRACKED_IPS = 500;

    private final ClientIpResolver clientIpResolver;
    private final ConcurrentHashMap<String, IpLockoutState> ipLockouts = new ConcurrentHashMap<>();
    private final AtomicInteger trackedIpCount = new AtomicInteger(0);
    private final AtomicBoolean isPruning = new AtomicBoolean(false);

    private static class IpLockoutState {
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private volatile long lockoutUntil = 0L;
        private volatile long firstFailureTime = 0L;
        private volatile boolean detached = false;

        boolean isLocked(long now) {
            return lockoutUntil > 0L && now < lockoutUntil;
        }

        long getRemainingLockoutSeconds(long now) {
            if (!isLocked(now)) {
                return 0L;
            }
            long remainingMs = lockoutUntil - now;
            return Math.max(1, (remainingMs + 999) / 1000L);
        }

        boolean isExpired(long now) {
            if (lockoutUntil > 0L) {
                return now >= lockoutUntil;
            }
            return firstFailureTime > 0L && (now - firstFailureTime >= LOCKOUT_DURATION_MS);
        }

        void markDetached() {
            this.detached = true;
        }

        boolean isDetached() {
            return this.detached;
        }
    }

    public AdminPasswordRateLimiter(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    /**
     * Checks whether password verification is currently locked for this session or client IP.
     * If capacity is saturated with unexpired entries, fails closed for untracked IPs.
     *
     * @param request current HTTP servlet request
     * @return remaining lockout duration in seconds, or 0 if not locked
     */
    public long getLockoutRemainingSeconds(HttpServletRequest request) {
        long now = System.currentTimeMillis();

        // 1. Check session-level lockout
        HttpSession session = request != null ? request.getSession(false) : null;
        if (session != null) {
            Object attr = session.getAttribute(SESSION_ATTR_LOCKOUT_UNTIL);
            if (attr instanceof Long sessionLockoutUntil) {
                if (now < sessionLockoutUntil) {
                    long remainingMs = sessionLockoutUntil - now;
                    return Math.max(1, (remainingMs + 999) / 1000L);
                } else {
                    // Lockout expired, reset session counters
                    session.removeAttribute(SESSION_ATTR_LOCKOUT_UNTIL);
                    session.setAttribute(SESSION_ATTR_FAILED_ATTEMPTS, 0);
                }
            }
        }

        // 2. Check IP-level lockout
        if (request != null && clientIpResolver != null) {
            String clientIp = clientIpResolver.resolveClientIp(request);
            String key = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "unknown";

            IpLockoutState ipState = ipLockouts.get(key);
            if (ipState != null) {
                if (ipState.isLocked(now)) {
                    return ipState.getRemainingLockoutSeconds(now);
                }
            } else {
                // Untracked IP: check if map is at capacity
                triggerPruningIfFull(now);
                if (trackedIpCount.get() >= MAX_TRACKED_IPS && !ipLockouts.containsKey(key)) {
                    log.warn("Admin password rate limiter at maximum capacity ({} tracked IPs). Failing closed for untracked IP: {}",
                            MAX_TRACKED_IPS, ClientIpResolver.maskIp(key));
                    return LOCKOUT_DURATION_MS / 1000L;
                }
            }
        }

        return 0L;
    }

    /**
     * Records a failed password verification attempt in both session and IP trackers.
     * Concurrency is strictly bounded to MAX_TRACKED_IPS via CAS reservation.
     * Never evicts unexpired entries. If capacity is exhausted, fails closed.
     *
     * @param request current HTTP servlet request
     * @return remaining lockout duration in seconds if locked out, or 0 if still within limit
     */
    public long recordFailure(HttpServletRequest request) {
        long now = System.currentTimeMillis();
        long lockoutUntil = now + LOCKOUT_DURATION_MS;
        boolean sessionLocked = false;

        // 1. Record session failure
        if (request != null) {
            HttpSession session = request.getSession(true);
            Object attr = session.getAttribute(SESSION_ATTR_FAILED_ATTEMPTS);
            int currentFailures = (attr instanceof Integer i ? i : 0) + 1;
            session.setAttribute(SESSION_ATTR_FAILED_ATTEMPTS, currentFailures);

            if (currentFailures >= MAX_FAILED_ATTEMPTS) {
                session.setAttribute(SESSION_ATTR_LOCKOUT_UNTIL, lockoutUntil);
                sessionLocked = true;
            }
        }

        // 2. Record IP failure
        boolean ipLocked = false;
        if (request != null && clientIpResolver != null) {
            String clientIp = clientIpResolver.resolveClientIp(request);
            String key = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "unknown";

            while (true) {
                IpLockoutState ipState = ipLockouts.get(key);
                if (ipState == null) {
                    triggerPruningIfFull(now);
                    ipState = ipLockouts.get(key);
                    if (ipState == null) {
                        if (!tryReserveCapacity()) {
                            log.warn("Admin password rate limiter capacity reached ({} IPs). Failing closed for IP: {}",
                                    MAX_TRACKED_IPS, ClientIpResolver.maskIp(key));
                            ipLocked = true;
                            break;
                        }

                        try {
                            IpLockoutState newState = new IpLockoutState();
                            IpLockoutState existing = ipLockouts.putIfAbsent(key, newState);
                            if (existing != null) {
                                // Concurrent thread inserted same IP simultaneously
                                trackedIpCount.decrementAndGet();
                                ipState = existing;
                            } else {
                                ipState = newState;
                            }
                        } catch (Throwable t) {
                            trackedIpCount.decrementAndGet();
                            throw t;
                        }
                    }
                }

                synchronized (ipState) {
                    if (ipState.isDetached()) {
                        // Concurrently pruned as expired; retry with a fresh lookup
                        continue;
                    }
                    if (ipState.firstFailureTime == 0L || (now - ipState.firstFailureTime > LOCKOUT_DURATION_MS && !ipState.isLocked(now))) {
                        ipState.firstFailureTime = now;
                        ipState.failureCount.set(0);
                    }
                    int count = ipState.failureCount.incrementAndGet();
                    if (count >= MAX_FAILED_ATTEMPTS) {
                        ipState.lockoutUntil = lockoutUntil;
                        ipLocked = true;
                    }
                }
                break;
            }
        }

        if (sessionLocked || ipLocked) {
            return LOCKOUT_DURATION_MS / 1000L;
        }
        return 0L;
    }

    /**
     * Resets failure counters upon successful password verification.
     *
     * @param request current HTTP servlet request
     */
    public void recordSuccess(HttpServletRequest request) {
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(SESSION_ATTR_FAILED_ATTEMPTS);
                session.removeAttribute(SESSION_ATTR_LOCKOUT_UNTIL);
            }
            if (clientIpResolver != null) {
                String clientIp = clientIpResolver.resolveClientIp(request);
                String key = (clientIp != null && !clientIp.isBlank()) ? clientIp.trim() : "unknown";
                IpLockoutState existing = ipLockouts.remove(key);
                if (existing != null) {
                    synchronized (existing) {
                        existing.markDetached();
                    }
                    trackedIpCount.decrementAndGet();
                }
            }
        }
    }

    private boolean tryReserveCapacity() {
        int current = trackedIpCount.get();
        while (current < MAX_TRACKED_IPS) {
            if (trackedIpCount.compareAndSet(current, current + 1)) {
                return true;
            }
            current = trackedIpCount.get();
        }
        return false;
    }

    private void triggerPruningIfFull(long now) {
        if (trackedIpCount.get() >= MAX_TRACKED_IPS && isPruning.compareAndSet(false, true)) {
            try {
                // Strictly prune ONLY entries whose state has expired.
                // Never evict unexpired entries to prevent quota resets and attacker-driven churn.
                for (Map.Entry<String, IpLockoutState> entry : ipLockouts.entrySet()) {
                    IpLockoutState state = entry.getValue();
                    synchronized (state) {
                        if (state.isExpired(now)) {
                            if (ipLockouts.remove(entry.getKey(), state)) {
                                state.markDetached();
                                trackedIpCount.decrementAndGet();
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Error during AdminPasswordRateLimiter expired entry pruning: {}", ex.getMessage());
            } finally {
                isPruning.set(false);
            }
        }
    }

    public int getTrackedIpCount() {
        return trackedIpCount.get();
    }

    public void reset() {
        ipLockouts.clear();
        trackedIpCount.set(0);
    }
}
