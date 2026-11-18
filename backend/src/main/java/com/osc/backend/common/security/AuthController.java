package com.osc.backend.common.security;

import com.osc.backend.common.dto.AuthStatusResponseDto;
import com.osc.backend.common.dto.CsrfTokenResponseDto;
import com.osc.backend.user.dto.UserDto;
import com.osc.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller exposing authentication status, current user profile, CSRF tokens,
 * and session logout endpoints.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication and CSRF security metadata")
public class AuthController {

    private final CurrentUserProvider currentUserProvider;
    private final UserService userService;

    public AuthController(CurrentUserProvider currentUserProvider, UserService userService) {
        this.currentUserProvider = currentUserProvider;
        this.userService = userService;
    }

    @GetMapping("/csrf")
    @Operation(summary = "Get CSRF Token", description = "Exposes active CSRF token details for browser-based session requests")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active CSRF token returned successfully"),
            @ApiResponse(responseCode = "204", description = "No CSRF token present on request")
    })
    public ResponseEntity<CsrfTokenResponseDto> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new CsrfTokenResponseDto(
                csrfToken.getHeaderName(),
                csrfToken.getParameterName(),
                csrfToken.getToken()
        ));
    }

    @GetMapping("/status")
    @Operation(summary = "Get Auth Status", description = "Returns whether the current session has an authenticated user")
    @ApiResponse(responseCode = "200", description = "Authentication status returned successfully")
    public ResponseEntity<AuthStatusResponseDto> getAuthStatus() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            if (auth.getPrincipal() instanceof AppUserPrincipal principal) {
                if (principal.getUserId() != null && userService.existsById(principal.getUserId())) {
                    return ResponseEntity.ok(AuthStatusResponseDto.authenticated(principal.getUserId(), principal.getUsername()));
                }
                // User account was deleted - clear orphan context
                SecurityContextHolder.clearContext();
            }
        }
        return ResponseEntity.ok(AuthStatusResponseDto.unauthenticated());
    }

    @GetMapping("/me")
    @Operation(summary = "Get Current User Profile", description = "Returns the user profile of the currently authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile returned successfully"),
            @ApiResponse(responseCode = "401", description = "User is not authenticated")
    })
    public ResponseEntity<UserDto> getCurrentUser() {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        UserDto userDto = userService.findById(currentUserId);
        return ResponseEntity.ok(userDto);
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout", description = "Invalidates the current session and clears security context")
    @ApiResponse(responseCode = "200", description = "Logged out successfully")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
