package com.osc.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.common.dto.ErrorResponseDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hardened rate-limiting filter for OAuth 2.0 flows.
 *
 * <p>Architectural Invariants &amp; Separation:
 * <ul>
 *   <li>OAuth Initiation ({@code /oauth2/authorization/**}) and OAuth Callback ({@code /login/oauth2/code/**})
 *       use completely independent rate-limiting buckets and limits.</li>
 *   <li>Initiation traffic cannot exhaust the callback bucket or block legitimate callbacks returning from GitHub.</li>
 *   <li>Callbacks retain their own independent abuse bucket to protect against authorization code flood attacks.</li>
 *   <li>Spring Security OAuth2 state validation ({@code state} parameter matching session state) remains 100% active.
 *       Invalid, missing, or mismatched callbacks continue to be rejected by Spring Security's authentication filter.</li>
 *   <li>Exceeded limits return HTTP 429 Too Many Requests with a RFC 7231 {@code Retry-After} header and structured JSON.</li>
 * </ul>
 */
public class OAuthLoginRateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(OAuthLoginRateLimitingFilter.class);

    public static final int DEFAULT_INITIATION_MAX_ATTEMPTS = 15;
    public static final long DEFAULT_INITIATION_WINDOW_SECONDS = 900L; // 15 minutes
    public static final int DEFAULT_CALLBACK_MAX_ATTEMPTS = 30;
    public static final long DEFAULT_CALLBACK_WINDOW_SECONDS = 900L; // 15 minutes
    public static final int DEFAULT_MAX_TRACKED_IPS = 5000;

    private final ClientIpResolver clientIpResolver;
    private final ObjectMapper objectMapper;

    private final int initiationMaxAttempts;
    private final long initiationWindowMs;
    private final int callbackMaxAttempts;
    private final long callbackWindowMs;
    private final int maxTrackedIps;

    // Independent in-memory buckets for initiation and callback flows
    private final ConcurrentHashMap<String, AttemptBucket> initiationBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptBucket> callbackBuckets = new ConcurrentHashMap<>();

    private static class AttemptBucket {
        private long windowStartEpochMs;
        private int count;

        AttemptBucket(long now) {
            this.windowStartEpochMs = now;
            this.count = 0;
        }

        synchronized boolean tryAcquire(long now, int limit, long windowMs) {
            if (now - windowStartEpochMs >= windowMs) {
                windowStartEpochMs = now;
                count = 0;
            }
            if (count < limit) {
                count++;
                return true;
            }
            return false;
        }

        synchronized long getRetryAfterSeconds(long now, long windowMs) {
            long remainingMs = (windowStartEpochMs + windowMs) - now;
            return Math.max(1, (remainingMs + 999) / 1000L);
        }

        synchronized boolean isExpired(long now, long windowMs) {
            return now - windowStartEpochMs >= windowMs;
        }
    }

    public OAuthLoginRateLimitingFilter(ClientIpResolver clientIpResolver, ObjectMapper objectMapper) {
        this(
                clientIpResolver,
                objectMapper,
                DEFAULT_INITIATION_MAX_ATTEMPTS,
                DEFAULT_INITIATION_WINDOW_SECONDS,
                DEFAULT_CALLBACK_MAX_ATTEMPTS,
                DEFAULT_CALLBACK_WINDOW_SECONDS,
                DEFAULT_MAX_TRACKED_IPS
        );
    }

    public OAuthLoginRateLimitingFilter(ClientIpResolver clientIpResolver, ObjectMapper objectMapper, int maxAttempts) {
        this(
                clientIpResolver,
                objectMapper,
                maxAttempts,
                DEFAULT_INITIATION_WINDOW_SECONDS,
                DEFAULT_CALLBACK_MAX_ATTEMPTS,
                DEFAULT_CALLBACK_WINDOW_SECONDS,
                DEFAULT_MAX_TRACKED_IPS
        );
    }

    public OAuthLoginRateLimitingFilter(
            ClientIpResolver clientIpResolver,
            ObjectMapper objectMapper,
            int initiationMaxAttempts,
            long initiationWindowSeconds,
            int callbackMaxAttempts,
            long callbackWindowSeconds
    ) {
        this(
                clientIpResolver,
                objectMapper,
                initiationMaxAttempts,
                initiationWindowSeconds,
                callbackMaxAttempts,
                callbackWindowSeconds,
                DEFAULT_MAX_TRACKED_IPS
        );
    }

    public OAuthLoginRateLimitingFilter(
            ClientIpResolver clientIpResolver,
            ObjectMapper objectMapper,
            int initiationMaxAttempts,
            long initiationWindowSeconds,
            int callbackMaxAttempts,
            long callbackWindowSeconds,
            int maxTrackedIps
    ) {
        this.clientIpResolver = clientIpResolver;
        this.objectMapper = objectMapper;
        this.initiationMaxAttempts = Math.max(1, initiationMaxAttempts);
        this.initiationWindowMs = Math.max(1, initiationWindowSeconds) * 1000L;
        this.callbackMaxAttempts = Math.max(1, callbackMaxAttempts);
        this.callbackWindowMs = Math.max(1, callbackWindowSeconds) * 1000L;
        this.maxTrackedIps = Math.max(100, maxTrackedIps);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();

        if (isInitiationEndpoint(uri)) {
            String clientIp = clientIpResolver.resolveClientIp(request);
            long now = System.currentTimeMillis();

            pruneExpiredBucketsIfFull(initiationBuckets, now, initiationWindowMs);

            AttemptBucket bucket = initiationBuckets.computeIfAbsent(clientIp, ip -> new AttemptBucket(now));
            if (!bucket.tryAcquire(now, initiationMaxAttempts, initiationWindowMs)) {
                long retryAfter = bucket.getRetryAfterSeconds(now, initiationWindowMs);
                log.warn("OAuth login initiation rate limit exceeded for clientIp={}, URI={}. Retry-After={}s", clientIp, uri, retryAfter);
                sendRateLimitResponse(response, uri, retryAfter, "OAuth login initiation rate limit exceeded. Please wait before retrying.");
                return;
            }
        } else if (isCallbackEndpoint(uri)) {
            String clientIp = clientIpResolver.resolveClientIp(request);
            long now = System.currentTimeMillis();

            pruneExpiredBucketsIfFull(callbackBuckets, now, callbackWindowMs);

            AttemptBucket bucket = callbackBuckets.computeIfAbsent(clientIp, ip -> new AttemptBucket(now));
            if (!bucket.tryAcquire(now, callbackMaxAttempts, callbackWindowMs)) {
                long retryAfter = bucket.getRetryAfterSeconds(now, callbackWindowMs);
                log.warn("OAuth callback rate limit exceeded for clientIp={}, URI={}. Retry-After={}s", clientIp, uri, retryAfter);
                sendRateLimitResponse(response, uri, retryAfter, "OAuth callback rate limit exceeded. Please wait before retrying.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isInitiationEndpoint(String uri) {
        return uri != null && uri.startsWith("/oauth2/authorization");
    }

    private boolean isCallbackEndpoint(String uri) {
        return uri != null && uri.startsWith("/login/oauth2/code");
    }

    private void sendRateLimitResponse(
            HttpServletResponse response,
            String uri,
            long retryAfter,
            String message
    ) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));

        ErrorResponseDto errorResponse = new ErrorResponseDto(
                OffsetDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                message,
                uri
        );
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private void pruneExpiredBucketsIfFull(ConcurrentHashMap<String, AttemptBucket> bucketsMap, long now, long windowMs) {
        if (bucketsMap.size() >= maxTrackedIps) {
            bucketsMap.entrySet().removeIf(entry -> entry.getValue().isExpired(now, windowMs));
        }
    }

    public int getInitiationBucketCount() {
        return initiationBuckets.size();
    }

    public int getCallbackBucketCount() {
        return callbackBuckets.size();
    }
}
