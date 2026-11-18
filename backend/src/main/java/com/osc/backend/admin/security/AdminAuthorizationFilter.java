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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

/**
 * Centralized Spring Security filter that enforces server-side password session authorization
 * on all administrative endpoints ({@code /api/v1/admin/**}).
 *
 * <p>Enforcement Rules:
 * <ol>
 *   <li>{@code /api/v1/admin/auth/status}, {@code /api/v1/admin/auth/verify-password}, and
 *       {@code /api/v1/admin/auth/logout} are permitted to check status, verify credentials, or terminate session.</li>
 *   <li>All other {@code /api/v1/admin/**} data endpoints require server-side session password verification
 *       ({@code SESSION_ATTR_ADMIN_PASSWORD_VERIFIED = true}).</li>
 *   <li>Rejects unverified requests with HTTP 401 Unauthorized before reaching REST controllers.</li>
 * </ol>
 */
public class AdminAuthorizationFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final long adminGithubId;

    public AdminAuthorizationFilter(ObjectMapper objectMapper, long adminGithubId) {
        this.objectMapper = objectMapper;
        this.adminGithubId = adminGithubId;
    }

    public AdminAuthorizationFilter(ObjectMapper objectMapper) {
        this(objectMapper, 0L);
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

        // 1. Status, verify-password, and logout endpoints are exempt from data-access authorization
        if (path.equals("/api/v1/admin/auth/status")
                || path.equals("/api/v1/admin/auth/verify-password")
                || path.equals("/api/v1/admin/auth/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. All other administrative data endpoints require complete password session authorization
        HttpSession session = request.getSession(false);
        if (session == null || !Boolean.TRUE.equals(session.getAttribute(AdminSecurityService.SESSION_ATTR_ADMIN_PASSWORD_VERIFIED))) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Admin authentication required", path);
            return;
        }

        // 3. Ensure the SecurityContext has an authenticated admin principal for downstream filters/controllers
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth == null || !existingAuth.isAuthenticated() || !(existingAuth.getPrincipal() instanceof AppUserPrincipal)) {
            AppUserPrincipal adminPrincipal = new AppUserPrincipal(
                    null,
                    adminGithubId > 0 ? adminGithubId : 0L,
                    "admin",
                    Collections.emptyMap(),
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
            Authentication adminAuth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(adminAuth);
            SecurityContextHolder.setContext(context);
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
        int queryIndex = path.indexOf('?');
        if (queryIndex != -1) {
            path = path.substring(0, queryIndex);
        }
        while (path.length() > 1 && path.endsWith("/")) {
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
        response.setCharacterEncoding("UTF-8");

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
