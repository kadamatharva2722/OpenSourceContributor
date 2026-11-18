package com.osc.backend.common.security;

import com.osc.backend.common.exception.InvalidRequestException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Authoritative client IP address resolver.
 *
 * <p>Security &amp; Architectural Invariants:
 * <ul>
 *   <li>On Render public web service deployments, inbound traffic routes through Cloudflare edge.
 *       When enabled via {@code app.security.rate-limit.trust-render-cloudflare-header=true},
 *       the authoritative client IP is retrieved from the {@code CF-Connecting-IP} header, which
 *       Cloudflare overwrites with the authentic client TCP connecting IP.</li>
 *   <li>For public submission endpoints, when Render Cloudflare trust is enabled, a missing or invalid
 *       {@code CF-Connecting-IP} header strictly rejects the request with HTTP 400 Bad Request before
 *       creating or consuming any rate-limit bucket, preventing shared proxy exhaustion.</li>
 *   <li>By default, Render Cloudflare header trust is DISABLED ({@code false}) for defense-in-depth,
 *       preventing spoofing in local development or environments with direct ingress.</li>
 *   <li>Only trusts forwarding headers ({@code X-Forwarded-For}) when the immediate socket connection
 *       ({@code request.getRemoteAddr()}) originates from an explicitly configured trusted proxy IP or CIDR block.</li>
 *   <li>Never accepts arbitrary client-supplied {@code X-Forwarded-For} values from direct, untrusted connections.</li>
 *   <li>When behind an authorized trusted proxy, parses {@code X-Forwarded-For} from <b>right to left</b>
 *       (from the closest proxy backwards) to determine the first untrusted IP address. Any client-injected
 *       spoofed IPs at the left of the chain are strictly discarded.</li>
 *   <li>Does not blindly trust all private network ranges by default; trusted proxies must be explicitly
 *       configured via {@code app.security.rate-limit.trusted-proxies} (e.g. {@code APP_TRUSTED_PROXIES}).</li>
 *   <li>In local development or when unconfigured, falls back securely to the direct socket address
 *       ({@code request.getRemoteAddr()}), ensuring local development functions seamlessly without configuration.</li>
 *   <li>Performs strictly structural validation on IPv4 and IPv6 literals without executing DNS lookups.</li>
 * </ul>
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    private static final Pattern IPV4_PATTERN = Pattern.compile(
            "^(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)\\."
          + "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)\\."
          + "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)\\."
          + "(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]\\d|\\d)$"
    );

    private final List<ProxyMatcher> trustedProxyMatchers;
    private final boolean trustRenderCloudflareHeader;

    public interface ProxyMatcher {
        boolean matches(InetAddress address, String ipString);
    }

    private static class ExactIpMatcher implements ProxyMatcher {
        private final InetAddress expectedAddress;
        private final String expectedString;

        ExactIpMatcher(InetAddress address, String ipString) {
            this.expectedAddress = address;
            this.expectedString = ipString;
        }

        @Override
        public boolean matches(InetAddress address, String ipString) {
            if (expectedAddress != null && address != null) {
                return expectedAddress.equals(address);
            }
            return expectedString != null && expectedString.equalsIgnoreCase(ipString);
        }
    }

    private static class CidrSubnetMatcher implements ProxyMatcher {
        private final byte[] networkBytes;
        private final int prefixLength;

        CidrSubnetMatcher(InetAddress networkAddress, int prefixLength) {
            this.networkBytes = networkAddress.getAddress();
            this.prefixLength = prefixLength;
        }

        @Override
        public boolean matches(InetAddress targetAddress, String ipString) {
            if (targetAddress == null) {
                return false;
            }
            byte[] targetBytes = targetAddress.getAddress();
            if (targetBytes.length != networkBytes.length) {
                return false; // IPv4 vs IPv6 length mismatch
            }

            for (int i = 0; i < networkBytes.length; i++) {
                int byteMask;
                if (prefixLength >= (i + 1) * 8) {
                    byteMask = 0xFF;
                } else if (prefixLength > i * 8) {
                    int remainingBits = prefixLength - (i * 8);
                    byteMask = (0xFF << (8 - remainingBits)) & 0xFF;
                } else {
                    byteMask = 0x00;
                }

                if ((targetBytes[i] & byteMask) != (networkBytes[i] & byteMask)) {
                    return false;
                }
            }
            return true;
        }
    }

    public ClientIpResolver() {
        this("", false);
    }

    public ClientIpResolver(String trustedProxiesConfig) {
        this(trustedProxiesConfig, false);
    }

    @Autowired
    public ClientIpResolver(
            @Value("${app.security.rate-limit.trusted-proxies:}") String trustedProxiesConfig,
            @Value("${app.security.rate-limit.trust-render-cloudflare-header:false}") boolean trustRenderCloudflareHeader
    ) {
        this.trustRenderCloudflareHeader = trustRenderCloudflareHeader;
        if (trustedProxiesConfig == null || trustedProxiesConfig.isBlank()) {
            this.trustedProxyMatchers = Collections.emptyList();
            log.info("Initialized ClientIpResolver with 0 trusted proxies (Direct connection mode). Render Cloudflare header trust: {}",
                    trustRenderCloudflareHeader);
        } else {
            List<ProxyMatcher> matchers = new ArrayList<>();
            String[] tokens = trustedProxiesConfig.split(",");
            for (String token : tokens) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    if (trimmed.contains("/")) {
                        String[] parts = trimmed.split("/");
                        if (parts.length == 2) {
                            String subnetIp = parts[0].trim();
                            int prefix = Integer.parseInt(parts[1].trim());
                            if (isValidIpLiteral(subnetIp)) {
                                InetAddress netAddr = InetAddress.getByName(subnetIp);
                                int maxPrefix = netAddr.getAddress().length * 8;
                                if (prefix >= 0 && prefix <= maxPrefix) {
                                    matchers.add(new CidrSubnetMatcher(netAddr, prefix));
                                } else {
                                    log.warn("Invalid CIDR prefix length '{}' in trusted proxy config", trimmed);
                                }
                            }
                        }
                    } else {
                        if (isValidIpLiteral(trimmed)) {
                            InetAddress addr = InetAddress.getByName(trimmed);
                            matchers.add(new ExactIpMatcher(addr, trimmed));
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Invalid trusted proxy configuration entry '{}': {}", trimmed, ex.getMessage());
                }
            }
            this.trustedProxyMatchers = Collections.unmodifiableList(matchers);
            log.info("Initialized ClientIpResolver with {} trusted proxy rule(s). Render Cloudflare header trust: {}",
                    this.trustedProxyMatchers.size(), trustRenderCloudflareHeader);
        }
    }

    /**
     * Resolves client IP address strictly for public submission endpoints
     * (e.g. /api/v1/contact, /api/v1/reports/issues, /api/v1/requests/repositories).
     *
     * <p>Security &amp; Rejection Invariants:
     * <ul>
     *   <li>When {@code trustRenderCloudflareHeader} is true (Render production mode),
     *       requires a valid, single IPv4 or IPv6 literal in {@code CF-Connecting-IP}.</li>
     *   <li>If {@code CF-Connecting-IP} is missing or invalid (e.g. malformed IPv6, hostnames,
     *       comma-separated values, ports, or whitespace tricks), strictly rejects the request
     *       by throwing {@link InvalidRequestException} (mapped to HTTP 400 Bad Request).
     *       It does NOT fall back to {@code request.getRemoteAddr()}, preventing shared proxy
     *       rate-limit exhaustion.</li>
     *   <li>Rejection occurs BEFORE any rate-limit bucket is allocated or consumed.</li>
     *   <li>When {@code trustRenderCloudflareHeader} is false (local development mode),
     *       falls back securely to direct socket {@code request.getRemoteAddr()} (or trusted proxy traversal
     *       if configured), preserving seamless local development without requiring spoofed headers.</li>
     *   <li>Never performs DNS lookups during validation.</li>
     * </ul>
     *
     * @param request current HTTP servlet request
     * @return validated client IP literal
     * @throws InvalidRequestException if Render Cloudflare trust is enabled and header is missing/invalid
     */
    public String resolvePublicSubmissionClientIp(HttpServletRequest request) {
        if (request == null) {
            throw new InvalidRequestException("Missing request context for client IP resolution.");
        }

        if (this.trustRenderCloudflareHeader) {
            HttpServletRequest rootRequest = getRootHttpRequest(request);
            String cfConnectingIp = rootRequest.getHeader("CF-Connecting-IP");
            if (cfConnectingIp == null || cfConnectingIp.isBlank()) {
                cfConnectingIp = request.getHeader("CF-Connecting-IP");
            }

            if (cfConnectingIp == null || cfConnectingIp.isBlank()) {
                log.warn("Public submission rejected: Missing required CF-Connecting-IP header on Render-trusted ingress.");
                throw new InvalidRequestException("Missing required CF-Connecting-IP header.");
            }

            // Do not strip ports. Strictly validate the raw header value (trimmed of HTTP protocol whitespace).
            String trimmed = cfConnectingIp.trim();
            if (!isValidIpLiteral(trimmed)) {
                log.warn("Public submission rejected: Invalid CF-Connecting-IP literal received: '{}'", maskIp(trimmed));
                throw new InvalidRequestException("Invalid CF-Connecting-IP header format.");
            }

            return normalizeIp(trimmed);
        }

        // Local development / non-Render mode: standard safe socket or trusted proxy resolution
        return resolveClientIp(request);
    }

    /**
     * Resolves the verified client IP address.
     *
     * <p>Security &amp; Forwarded-Header Handling:
     * <ul>
     *   <li>Unwraps any {@link ServletRequestWrapper} decorators (such as Spring's
     *       {@code ForwardedHeaderFilter$ForwardedHeaderExtractingRequest}) to access the root container request
     *       and authentic physical TCP socket address ({@code rootRequest.getRemoteAddr()}).</li>
     *   <li>If {@code trustRenderCloudflareHeader} is enabled, prioritizes {@code CF-Connecting-IP}
     *       after strictly validating that it is a valid IPv4/IPv6 literal without stripping ports.</li>
     *   <li>If the immediate socket peer is NOT an explicitly trusted proxy, any forwarded headers
     *       ({@code X-Forwarded-For}) are strictly ignored, preventing header spoofing attacks.</li>
     *   <li>When behind an authorized trusted proxy, parses {@code X-Forwarded-For} from right to left
     *       to locate the first untrusted IP address.</li>
     * </ul>
     *
     * @param request current HTTP servlet request
     * @return authoritative client IP address
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }

        // Unwrap request decorators to reach the root container request.
        // This ensures getRemoteAddr() returns the authentic physical socket peer,
        // even when Spring's ForwardedHeaderFilter has rewritten the request wrapper.
        HttpServletRequest rootRequest = getRootHttpRequest(request);

        String remoteAddr = rootRequest.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isBlank()) {
            remoteAddr = request.getRemoteAddr();
        }
        if (remoteAddr == null || remoteAddr.isBlank()) {
            remoteAddr = "127.0.0.1";
        }
        remoteAddr = normalizeIp(cleanIpCandidate(remoteAddr));

        // 1. If configured to trust Render Cloudflare ingress:
        // On Render public web services, all inbound traffic passes through Cloudflare edge.
        // Cloudflare terminates the TCP connection and injects the authentic client IP into CF-Connecting-IP,
        // overwriting any client-supplied header.
        if (this.trustRenderCloudflareHeader) {
            String cfConnectingIp = rootRequest.getHeader("CF-Connecting-IP");
            if (cfConnectingIp == null || cfConnectingIp.isBlank()) {
                cfConnectingIp = request.getHeader("CF-Connecting-IP");
            }

            if (cfConnectingIp != null && !cfConnectingIp.isBlank()) {
                String trimmed = cfConnectingIp.trim();
                // Strictly validate format without stripping ports
                if (isValidIpLiteral(trimmed)) {
                    return normalizeIp(trimmed);
                } else {
                    log.warn("Invalid CF-Connecting-IP header value received: '{}'. Falling back safely.", maskIp(trimmed));
                }
            } else {
                log.debug("CF-Connecting-IP header absent on Render-trusted request. Falling back safely.");
            }

            // Safe fallback: CF-Connecting-IP was absent or invalid.
            // Do NOT trust X-Forwarded-For unless the socket peer (remoteAddr) is an explicitly configured trusted proxy.
            if (!isTrustedProxy(remoteAddr)) {
                return remoteAddr;
            }
        } else {
            // Render Cloudflare header trust is disabled (default for local development and non-Render environments).
            // Direct socket remote address is used unless the peer is an explicitly trusted proxy.
            if (!isTrustedProxy(remoteAddr)) {
                return remoteAddr;
            }
        }

        // 2. Direct connection IS from an explicitly configured trusted proxy.
        // Inspect raw X-Forwarded-For header using right-to-left traversal.
        String forwardedFor = rootRequest.getHeader("X-Forwarded-For");
        if (forwardedFor == null || forwardedFor.isBlank()) {
            forwardedFor = request.getHeader("X-Forwarded-For");
        }
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return remoteAddr;
        }

        // Standard X-Forwarded-For format: client, proxy1, proxy2...
        // Walk right-to-left (from closest trusted proxy backwards) to find the first untrusted IP.
        String[] parts = forwardedFor.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = cleanIpCandidate(parts[i]);
            if (candidate.isEmpty() || !isValidIpLiteral(candidate)) {
                continue;
            }
            String normalizedCandidate = normalizeIp(candidate);
            if (!isTrustedProxy(normalizedCandidate)) {
                // First untrusted IP from the right is the verified client IP.
                return normalizedCandidate;
            }
        }

        // If all IPs in X-Forwarded-For are trusted proxies, return the leftmost valid one.
        for (String part : parts) {
            String candidate = cleanIpCandidate(part);
            if (!candidate.isEmpty() && isValidIpLiteral(candidate)) {
                return normalizeIp(candidate);
            }
        }

        return remoteAddr;
    }

    /**
     * Traverses nested {@link ServletRequestWrapper} instances to retrieve the root HTTP request.
     */
    private static HttpServletRequest getRootHttpRequest(HttpServletRequest request) {
        ServletRequest current = request;
        while (current instanceof ServletRequestWrapper wrapper) {
            ServletRequest inner = wrapper.getRequest();
            if (inner == null) {
                break;
            }
            current = inner;
        }
        if (current instanceof HttpServletRequest httpCurrent) {
            return httpCurrent;
        }
        return request;
    }

    /**
     * Cleans whitespace, square brackets (IPv6 literals), and trailing port numbers from IP candidates
     * when parsing proxy forwarding headers (X-Forwarded-For).
     */
    private static String cleanIpCandidate(String candidate) {
        if (candidate == null) {
            return "";
        }
        String trimmed = candidate.trim();
        // Remove brackets for IPv6 literals: e.g. [2001:db8::1] or [2001:db8::1]:8080
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            int closeIdx = trimmed.indexOf(']');
            trimmed = trimmed.substring(1, closeIdx);
        } else if (trimmed.contains(".") && trimmed.indexOf(':') > 0 && trimmed.indexOf(':') == trimmed.lastIndexOf(':')) {
            // IPv4 with port, e.g. 192.168.1.1:8080
            trimmed = trimmed.substring(0, trimmed.indexOf(':'));
        }
        return trimmed.trim();
    }

    /**
     * Checks whether an IP address matches any configured trusted proxy rule.
     *
     * @param ipStr candidate IP address string
     * @return true if trusted; false otherwise
     */
    public boolean isTrustedProxy(String ipStr) {
        if (ipStr == null || ipStr.isBlank() || trustedProxyMatchers.isEmpty()) {
            return false;
        }
        if (!isValidIpLiteral(ipStr)) {
            return false;
        }
        try {
            InetAddress addr = InetAddress.getByName(ipStr);
            for (ProxyMatcher matcher : trustedProxyMatchers) {
                if (matcher.matches(addr, ipStr)) {
                    return true;
                }
            }
        } catch (UnknownHostException ex) {
            return false;
        }
        return false;
    }

    /**
     * Validates whether a string is a strictly formatted IPv4 or IPv6 literal.
     * Completely avoids DNS lookups.
     */
    public static boolean isValidIpLiteral(String str) {
        if (str == null || str.isBlank()) {
            return false;
        }
        if (str.contains(" ")) {
            return false;
        }
        if (str.indexOf(':') == -1) {
            return isValidIpv4Literal(str);
        } else {
            return isValidIpv6Literal(str);
        }
    }

    public static boolean isValidIpv4Literal(String str) {
        if (str == null || str.length() < 7 || str.length() > 15) {
            return false;
        }
        if (!IPV4_PATTERN.matcher(str).matches()) {
            return false;
        }
        String[] octets = str.split("\\.");
        if (octets.length != 4) {
            return false;
        }
        for (String octet : octets) {
            try {
                int val = Integer.parseInt(octet);
                if (val < 0 || val > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    public static boolean isValidIpv6Literal(String str) {
        if (str == null || str.length() < 2 || str.length() > 45) {
            return false;
        }

        // Cannot contain whitespace, brackets, commas, or characters outside hex / colon / dot
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            boolean validChar = (c >= '0' && c <= '9') ||
                                (c >= 'a' && c <= 'f') ||
                                (c >= 'A' && c <= 'F') ||
                                c == ':' || c == '.';
            if (!validChar) {
                return false;
            }
        }

        // Handle IPv4-mapped IPv6, e.g. ::ffff:192.0.2.1
        if (str.contains(".")) {
            int lastColon = str.lastIndexOf(':');
            if (lastColon <= 0 || lastColon == str.length() - 1) {
                return false;
            }
            String ipv4Part = str.substring(lastColon + 1);
            if (!isValidIpv4Literal(ipv4Part)) {
                return false;
            }
            String ipv6Part = str.substring(0, lastColon);
            return isValidPureIpv6(ipv6Part.endsWith(":") ? ipv6Part + "0" : ipv6Part + ":0");
        }

        return isValidPureIpv6(str);
    }

    private static boolean isValidPureIpv6(String str) {
        if (str.contains(":::")) {
            return false;
        }

        // Check for multiple "::"
        int firstDoubleColon = str.indexOf("::");
        if (firstDoubleColon != -1) {
            int secondDoubleColon = str.indexOf("::", firstDoubleColon + 2);
            if (secondDoubleColon != -1) {
                return false; // Multiple "::" not allowed
            }
        }

        // Single colon at start or end is illegal
        if (str.startsWith(":") && !str.startsWith("::")) {
            return false;
        }
        if (str.endsWith(":") && !str.endsWith("::")) {
            return false;
        }

        if (firstDoubleColon != -1) {
            // Compressed format with "::"
            String left = str.substring(0, firstDoubleColon);
            String right = str.substring(firstDoubleColon + 2);

            int segmentCount = 0;
            if (!left.isEmpty()) {
                String[] leftParts = left.split(":");
                for (String part : leftParts) {
                    if (!isValidHexSegment(part)) {
                        return false;
                    }
                    segmentCount++;
                }
            }
            if (!right.isEmpty()) {
                String[] rightParts = right.split(":");
                for (String part : rightParts) {
                    if (!isValidHexSegment(part)) {
                        return false;
                    }
                    segmentCount++;
                }
            }
            return segmentCount <= 7;
        } else {
            // Full format without "::": must have exactly 8 segments
            String[] parts = str.split(":");
            if (parts.length != 8) {
                return false;
            }
            for (String part : parts) {
                if (!isValidHexSegment(part)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static boolean isValidHexSegment(String segment) {
        if (segment == null || segment.isEmpty() || segment.length() > 4) {
            return false;
        }
        for (int i = 0; i < segment.length(); i++) {
            char c = segment.charAt(i);
            boolean isHex = (c >= '0' && c <= '9') ||
                            (c >= 'a' && c <= 'f') ||
                            (c >= 'A' && c <= 'F');
            if (!isHex) {
                return false;
            }
        }
        return true;
    }

    static boolean isSafeIpLiteral(String str) {
        return isValidIpLiteral(str);
    }

    private static String normalizeIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "0.0.0.0";
        }
        String lower = ip.trim().toLowerCase();
        if ("0:0:0:0:0:0:0:1".equals(lower) || "::1".equals(lower)) {
            return "127.0.0.1";
        }
        return lower;
    }

    /**
     * Masks an IP address for safe logging without exposing full user IP addresses or PII.
     * IPv4: 198.51.100.42 -> 198.51.***
     * IPv6: 2001:0db8:85a3::1 -> 2001:0db8:***
     *
     * @param ip input IP address string
     * @return masked IP string suitable for logging
     */
    public static String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "unknown";
        }
        String trimmed = ip.trim();
        if (trimmed.contains(".")) {
            String[] parts = trimmed.split("\\.");
            if (parts.length >= 2) {
                return parts[0] + "." + parts[1] + ".***";
            }
            return "***";
        } else if (trimmed.contains(":")) {
            String[] parts = trimmed.split(":");
            if (parts.length >= 2) {
                return parts[0] + ":" + parts[1] + ":***";
            }
            return "***";
        }
        return "***";
    }

    /**
     * Visible for testing and diagnostic telemetry.
     */
    public int getTrustedRuleCount() {
        return trustedProxyMatchers.size();
    }

    /**
     * Visible for testing and diagnostic telemetry.
     */
    public boolean isTrustRenderCloudflareHeader() {
        return trustRenderCloudflareHeader;
    }
}
