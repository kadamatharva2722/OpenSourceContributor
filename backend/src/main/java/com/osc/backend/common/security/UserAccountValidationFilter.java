package com.osc.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.common.dto.ErrorResponseDto;
import com.osc.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;

/**
 * Centralized security filter that validates whether authenticated session principals
 * still correspond to an active, existing user record in the database.
 * Protects all protected endpoints against stale sessions after account deletion,
 * even when the in-memory SessionRegistry entries have been lost (e.g. server restart).
 *
 * Distinguishes between deleted accounts (HTTP 401 Unauthorized with session termination)
 * and temporary database outages (HTTP 503 Service Unavailable with session preserved).
 */
public class UserAccountValidationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(UserAccountValidationFilter.class);

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public UserAccountValidationFilter(UserRepository userRepository, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Strictly check only authenticated non-anonymous sessions; bypass unauthenticated/guest traffic with zero DB queries
        if (userRepository != null && auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getPrincipal() instanceof AppUserPrincipal principal) {
                Long userId = principal.getUserId();

                if (userId != null) {
                    boolean exists;
                    try {
                        exists = userRepository.existsById(userId);
                    } catch (Exception ex) {
                        // Safe logging: log exception class and user id only, avoiding any leakage of DB credentials, SQL, or PII
                        log.error("Database failure while validating user account existence for userId={}: {}",
                                userId, ex.getClass().getSimpleName());

                        // Controlled HTTP 503 Service Unavailable response using standard ErrorResponseDto
                        // Legitimate user sessions are NOT invalidated or cleared on transient database errors
                        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        ErrorResponseDto errorResponse = new ErrorResponseDto(
                                OffsetDateTime.now(),
                                HttpStatus.SERVICE_UNAVAILABLE.value(),
                                HttpStatus.SERVICE_UNAVAILABLE.getReasonPhrase(),
                                "Authentication verification temporarily unavailable. Please try again.",
                                request.getRequestURI()
                        );
                        objectMapper.writeValue(response.getOutputStream(), errorResponse);
                        return;
                    }

                    if (!exists) {
                        log.warn("Terminating stale session: user id={} does not exist in database (URI={})",
                                userId, request.getRequestURI());

                        // 1. Clear SecurityContext
                        SecurityContextHolder.clearContext();

                        // 2. Invalidate stale HTTP session where possible
                        HttpSession session = request.getSession(false);
                        if (session != null) {
                            try {
                                session.invalidate();
                            } catch (Exception ex) {
                                log.debug("Session already invalidated: {}", ex.getMessage());
                            }
                        }

                        // 3. Expire session cookie in client response
                        Cookie jsessionCookie = new Cookie("JSESSIONID", "");
                        jsessionCookie.setPath("/");
                        jsessionCookie.setMaxAge(0);
                        jsessionCookie.setHttpOnly(true);
                        response.addCookie(jsessionCookie);

                        // 4. Return HTTP 401 with standard ErrorResponseDto JSON format
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        ErrorResponseDto errorResponse = new ErrorResponseDto(
                                OffsetDateTime.now(),
                                HttpStatus.UNAUTHORIZED.value(),
                                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                                "User account has been deleted or no longer exists",
                                request.getRequestURI()
                        );
                        objectMapper.writeValue(response.getOutputStream(), errorResponse);
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
