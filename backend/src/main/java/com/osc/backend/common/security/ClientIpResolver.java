package com.osc.backend.common.security;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletRequestWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * </ul>
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    // Matches safe IPv4/IPv6 literal characters; prevents DNS lookups when parsing IP literals
    private static final Pattern SAFE_IP_LITERAL = Pattern.compile("^[0-9a-fA-F:.]+$");

    private final List<ProxyMatcher> trustedProxyMatchers;

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
        this("");
    }

    public ClientIpResolver(
            @Value("${app.security.rate-limit.trusted-proxies:}") String trustedProxiesConfig
    ) {
        if (trustedProxiesConfig == null || trustedProxiesConfig.isBlank()) {
            this.trustedProxyMatchers = Collections.emptyList();
            log.info("Initialized ClientIpResolver with 0 trusted proxies (Direct connection mode).");
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
                            if (isSafeIpLiteral(subnetIp)) {
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
                        if (isSafeIpLiteral(trimmed)) {
                            InetAddress addr = InetAddress.getByName(trimmed);
                            matchers.add(new ExactIpMatcher(addr, trimmed));
                        }
                    }
                } catch (Exception ex) {
                    log.warn("Invalid trusted proxy configuration entry '{}': {}", trimmed, ex.getMessage());
                }
            }
            this.trustedProxyMatchers = Collections.unmodifiableList(matchers);
            log.info("Initialized ClientIpResolver with {} trusted proxy rule(s).", this.trustedProxyMatchers.size());
        }
    }

    /**
     * Resolves the verified client IP address.
     *
     * <p>Security &amp; Forwarded-Header Handling:
     * <ul>
     *   <li>Unwraps any {@link ServletRequestWrapper} decorators (such as Spring's
     *       {@code ForwardedHeaderFilter$ForwardedHeaderExtractingRequest}) to access the root container request
     *       and authentic physical TCP socket address ({@code rootRequest.getRemoteAddr()}).</li>
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

        // 1. If direct peer connection is NOT an explicitly trusted proxy,
        // use socket remote address directly. Never inspect forwarded headers from untrusted connections.
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        // 2. Direct connection IS from a trusted proxy. Inspect raw X-Forwarded-For header.
        // Spring's ForwardedHeaderRemovingRequest strips X-Forwarded-For from the wrapper,
        // so we retrieve it from the root request first.
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
            if (candidate.isEmpty() || !isSafeIpLiteral(candidate)) {
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
            if (!candidate.isEmpty() && isSafeIpLiteral(candidate)) {
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
     * Cleans whitespace, square brackets (IPv6 literals), and trailing port numbers from IP candidates.
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
        if (!isSafeIpLiteral(ipStr)) {
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

    private static boolean isSafeIpLiteral(String str) {
        if (str == null || str.isBlank() || str.length() > 45) {
            return false;
        }
        return SAFE_IP_LITERAL.matcher(str).matches();
    }

    private static String normalizeIp(String ip) {
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }

    /**
     * Visible for testing and diagnostic telemetry.
     */
    public int getTrustedRuleCount() {
        return trustedProxyMatchers.size();
    }
}
