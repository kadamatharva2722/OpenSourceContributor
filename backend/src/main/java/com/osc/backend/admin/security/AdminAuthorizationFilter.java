package com.osc.backend.admin.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.common.dto.ErrorResponseDto;
import com.osc.backend.common.security.AppUserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Objects;

/**
 * Centralized Spring Security filter that enforces two-factor authorization on all
 * administrative endpoints ({@code /api/v1/admin/**}).
 *
 * <p>Enforcement Rules:
 * <ol>
 *   <li>{@code /api/v1/admin/auth/status} and {@code /api/v1/admin/auth/logout} are permitted to check state or terminate session.</li>
 *   <li>{@code /api/v1/admin/auth/verify-password} requires GitHub authentication with matching {@code APP_ADMIN_GITHUB_ID}.</li>
 *   <li>All other {@code /api/v1/admin/**} data endpoints require both GitHub admin ID match AND
 *       server-side session password verification ({@code SESSION_ATTR_ADMIN_PASSWORD_VERIFIED = true}).</li>
 *   <li>Rejects unverified requests with HTTP 401 or 403 before reaching REST controllers.</li>
 * </ol>
 */
public class AdminAuthorizationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final long adminGithubId;

    public AdminAuthorizationFilter(ObjectMapper objectMapper, long adminGithubId) {
        this.objectMapper = objectMapper;
        this.adminGithubId = adminGithubId;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String path = resolveNormalizedPath(request);

        if (!path.startsWith("/api/v1/admin")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1. Status and logout endpoints are exempt from data-access authorization
        if (path.equals("/api/v1/admin/auth/status") || path.equals("/api/v1/admin/auth/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract and verify authenticated principal
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Authentication required to access admin panel", path);
            return;
        }

        // 3. Verify permanent numeric GitHub ID against configured admin ID
        if (adminGithubId <= 0 || !Objects.equals(principal.getGithubId(), adminGithubId)) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "Access denied: Administrative privileges required", path);
            return;
        }

        // 4. Password verification endpoint requires valid admin GitHub ID (verified above), but not yet password session
        if (path.equals("/api/v1/admin/auth/verify-password")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 5. All other administrative data endpoints require complete two-factor session authorization
        HttpSession session = request.getSession(false);
        if (session == null || !Boolean.TRUE.equals(session.getAttribute(AdminSecurityService.SESSION_ATTR_ADMIN_PASSWORD_VERIFIED))) {
            sendErrorResponse(response, HttpStatus.FORBIDDEN, "Access denied: Admin password verification required in the current session", path);
            return;
        }

        // All checks passed
        filterChain.doFilter(request, response);
    }

    private String resolveNormalizedPath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isBlank()) {
            path = request.getRequestURI();
        }
        if (path == null) {
            return "";
        }
        path = path.replaceAll("/+", "/");
        if (path.length() > 1 && path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void sendErrorResponse(
            HttpServletResponse response,
            HttpStatus status,
            String message,
            String path
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponseDto errorDto = new ErrorResponseDto(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        objectMapper.writeValue(response.getOutputStream(), errorDto);
    }
}
