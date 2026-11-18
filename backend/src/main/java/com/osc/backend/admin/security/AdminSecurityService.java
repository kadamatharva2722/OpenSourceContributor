package com.osc.backend.admin.security;

import com.osc.backend.admin.dto.AdminStatusDto;
import com.osc.backend.admin.entity.AdminAuditLog;
import com.osc.backend.admin.exception.AdminLockoutException;
import com.osc.backend.admin.exception.InvalidAdminPasswordException;
import com.osc.backend.admin.repository.AdminAuditLogRepository;
import com.osc.backend.common.exception.UnauthenticatedException;
import com.osc.backend.common.security.AppUserPrincipal;
import com.osc.backend.common.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Dedicated administrator password authentication service:
 * <p>Enforces server-side password verification against configured BCrypt {@code ADMIN_PASSWORD_HASH},
 * session fixation protection, attempt tracking with temporary lockouts, and fail-closed posture
 * when credentials are missing or unconfigured.
 */
@Service
public class AdminSecurityService {

    private static final Logger log = LoggerFactory.getLogger(AdminSecurityService.class);

    public static final String SESSION_ATTR_ADMIN_PASSWORD_VERIFIED = "OSC_ADMIN_PASSWORD_VERIFIED";

    private static final Pattern BCRYPT_PATTERN = Pattern.compile("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$");

    private final CurrentUserProvider currentUserProvider;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminPasswordRateLimiter adminPasswordRateLimiter;
    private final long adminGithubId;
    private final String adminPasswordHash;

    public AdminSecurityService(
            CurrentUserProvider currentUserProvider,
            AdminAuditLogRepository adminAuditLogRepository,
            PasswordEncoder passwordEncoder,
            AdminPasswordRateLimiter adminPasswordRateLimiter,
            @Value("${app.security.admin.github-id:0}") long adminGithubId,
            @Value("${app.security.admin.password-hash:}") String adminPasswordHash
    ) {
        this.currentUserProvider = currentUserProvider;
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPasswordRateLimiter = adminPasswordRateLimiter;
        this.adminGithubId = adminGithubId;
        this.adminPasswordHash = adminPasswordHash != null ? adminPasswordHash.trim() : "";
    }

    /**
     * Checks if the caller is currently verified with administrator privileges.
     */
    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() &&
                auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    /**
     * Validates that the current user possesses an admin-verified session.
     */
    public AppUserPrincipal requireAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal;
        }
        throw new UnauthenticatedException("Authentication required to access admin panel");
    }

    /**
     * Enforces administrator authorization:
     * Server-side session contains verified password attribute (SESSION_ATTR_ADMIN_PASSWORD_VERIFIED = true).
     */
    public AppUserPrincipal requireAdminVerified(HttpServletRequest request) {
        if (!isPasswordVerified(request)) {
            log.warn("Admin data endpoint access rejected: password verification required in current session");
            throw new AccessDeniedException("Access denied: Admin password verification required in the current session");
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof AppUserPrincipal principal) {
            return principal;
        }

        return createDefaultAdminPrincipal();
    }

    /**
     * Checks whether the current HTTP session is verified with the administrator password.
     */
    public boolean isPasswordVerified(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(SESSION_ATTR_ADMIN_PASSWORD_VERIFIED));
    }

    /**
     * Returns the comprehensive authentication status of the administrator console.
     */
    public AdminStatusDto getAdminStatus(HttpServletRequest request) {
        boolean verified = isPasswordVerified(request);
        if (verified) {
            return new AdminStatusDto(true, true, "admin", adminGithubId > 0 ? adminGithubId : 0L, "ADMIN_VERIFIED", false);
        } else {
            return new AdminStatusDto(false, false, null, null, "UNAUTHENTICATED", false);
        }
    }

    /**
     * Backwards-compatible overload for components that do not pass HttpServletRequest.
     */
    public AdminStatusDto getAdminStatus() {
        return getAdminStatus(null);
    }

    /**
     * Verifies the administrator password against the configured BCrypt hash.
     * Primary admin authentication endpoint logic.
     *
     * @param rawPassword password entered by the user
     * @param request current HTTP request
     * @return updated AdminStatusDto reflecting verified admin status
     */
    public AdminStatusDto verifyAdminPassword(String rawPassword, HttpServletRequest request) {
        // Step 1: Check attempt lockout (session & IP)
        long lockoutRemaining = adminPasswordRateLimiter.getLockoutRemainingSeconds(request);
        if (lockoutRemaining > 0) {
            log.warn("Admin password verification rejected: temporarily locked out (remaining: {}s)", lockoutRemaining);
            throw new AdminLockoutException(lockoutRemaining);
        }

        // Step 2: Fail closed if ADMIN_PASSWORD_HASH is missing, blank, or invalid
        if (!isValidBcryptHash(adminPasswordHash)) {
            log.warn("Admin password verification attempted, but ADMIN_PASSWORD_HASH is not configured or is invalid. Failing closed.");
            recordSecurityAudit("admin", "FAILED_ADMIN_PASSWORD_VERIFICATION", "Password verification attempted while ADMIN_PASSWORD_HASH is unconfigured");
            throw new AccessDeniedException("Administrative access is disabled or unconfigured on this instance");
        }

        // Step 3: Verify password with BCrypt
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, adminPasswordHash)) {
            long lockoutSeconds = adminPasswordRateLimiter.recordFailure(request);
            recordSecurityAudit("admin", "FAILED_ADMIN_PASSWORD_VERIFICATION",
                    "Invalid password attempt" + (lockoutSeconds > 0 ? " (Account locked for 15 minutes)" : ""));
            log.warn("Admin password verification failed (lockoutSeconds: {})", lockoutSeconds);

            if (lockoutSeconds > 0) {
                throw new AdminLockoutException(lockoutSeconds);
            }
            throw new InvalidAdminPasswordException("Invalid administrator password");
        }

        // Step 4: Verification success
        adminPasswordRateLimiter.recordSuccess(request);

        // Step 5: Session fixation protection - rotate session ID
        AppUserPrincipal adminPrincipal = createDefaultAdminPrincipal();
        if (request != null) {
            request.changeSessionId();
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_ATTR_ADMIN_PASSWORD_VERIFIED, Boolean.TRUE);

            Authentication auth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        }

        log.info("Administrator password successfully verified");
        recordSecurityAudit("admin", "ADMIN_PASSWORD_VERIFIED", "Administrator password successfully verified");

        return new AdminStatusDto(true, true, "admin", adminGithubId > 0 ? adminGithubId : 0L, "ADMIN_VERIFIED", false);
    }

    /**
     * Terminates the administrative session, invalidating session state and clearing security context.
     */
    public void logout(HttpServletRequest request) {
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(SESSION_ATTR_ADMIN_PASSWORD_VERIFIED);
                session.invalidate();
            }
        }
        SecurityContextHolder.clearContext();
        recordSecurityAudit("admin", "ADMIN_LOGOUT", "Admin session logged out and invalidated");
    }

    private AppUserPrincipal createDefaultAdminPrincipal() {
        return new AppUserPrincipal(
                null,
                adminGithubId > 0 ? adminGithubId : 0L,
                "admin",
                Collections.emptyMap(),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private boolean isValidBcryptHash(String hash) {
        return hash != null && !hash.isBlank() && BCRYPT_PATTERN.matcher(hash).matches();
    }

    private void recordSecurityAudit(String username, String action, String details) {
        try {
            AdminAuditLog auditLog = new AdminAuditLog(
                    adminGithubId > 0 ? adminGithubId : 0L,
                    username != null ? username : "admin",
                    action,
                    "SECURITY",
                    String.valueOf(adminGithubId > 0 ? adminGithubId : 0L),
                    details
            );
            adminAuditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Failed to persist security audit log: {}", ex.getMessage());
        }
    }

    public long getAdminGithubId() {
        return adminGithubId;
    }
}
