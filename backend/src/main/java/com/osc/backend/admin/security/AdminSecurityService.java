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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Two-factor authorization service for the administrator console:
 * <ol>
 *   <li>Factor 1: Permanent GitHub numeric ID verification against configured {@code APP_ADMIN_GITHUB_ID}.</li>
 *   <li>Factor 2: Administrator password verification against configured BCrypt {@code ADMIN_PASSWORD_HASH}.</li>
 * </ol>
 *
 * <p>Enforces server-side session authorization, session fixation protection, attempt tracking with
 * temporary lockouts, and fail-closed posture when credentials are missing or unconfigured.
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
     * Checks if the currently authenticated user's GitHub ID matches the configured admin ID.
     */
    public boolean isAdmin() {
        if (adminGithubId <= 0) {
            return false;
        }
        try {
            AppUserPrincipal principal = currentUserProvider.getCurrentPrincipal();
            return principal != null && Objects.equals(principal.getGithubId(), adminGithubId);
        } catch (UnauthenticatedException ex) {
            return false;
        }
    }

    /**
     * Validates that the current user is authenticated and possesses the authorized admin GitHub ID.
     */
    public AppUserPrincipal requireAdmin() {
        AppUserPrincipal principal = currentUserProvider.getCurrentPrincipal();
        if (principal == null) {
            throw new UnauthenticatedException("Authentication required to access admin panel");
        }

        if (adminGithubId <= 0) {
            log.warn("Admin panel access attempted by user {} (githubId: {}), but APP_ADMIN_GITHUB_ID is not configured",
                    principal.getUsername(), principal.getGithubId());
            recordSecurityAudit(principal, "FAILED_ADMIN_ACCESS", "Admin access attempted while administrative privileges are disabled");
            throw new AccessDeniedException("Administrative privileges are currently disabled on this instance");
        }

        if (!Objects.equals(principal.getGithubId(), adminGithubId)) {
            log.warn("Unauthorized admin panel access attempt by user {} (githubId: {}). Expected admin githubId: {}",
                    principal.getUsername(), principal.getGithubId(), adminGithubId);
            recordSecurityAudit(principal, "FAILED_ADMIN_ACCESS", "Unauthorized admin access attempt by non-admin account");
            throw new AccessDeniedException("Access denied: Administrative privileges required");
        }

        return principal;
    }

    /**
     * Enforces complete two-factor administrator authorization:
     * 1. Authenticated GitHub user matches APP_ADMIN_GITHUB_ID.
     * 2. Server-side session contains verified password attribute (SESSION_ATTR_ADMIN_PASSWORD_VERIFIED = true).
     */
    public AppUserPrincipal requireAdminVerified(HttpServletRequest request) {
        AppUserPrincipal principal = requireAdmin();

        if (!isPasswordVerified(request)) {
            log.warn("Admin data endpoint access rejected for user {} (githubId: {}): password verification required in current session",
                    principal.getUsername(), principal.getGithubId());
            recordSecurityAudit(principal, "FAILED_ADMIN_ACCESS", "Admin data endpoint accessed without session password verification");
            throw new AccessDeniedException("Access denied: Admin password verification required in the current session");
        }

        return principal;
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
     * Returns the comprehensive multi-step authentication status of the current user.
     */
    public AdminStatusDto getAdminStatus(HttpServletRequest request) {
        try {
            AppUserPrincipal principal = currentUserProvider.getCurrentPrincipal();
            if (principal == null) {
                return new AdminStatusDto(false, false, null, null, "UNAUTHENTICATED", false);
            }

            if (adminGithubId <= 0 || !Objects.equals(principal.getGithubId(), adminGithubId)) {
                return new AdminStatusDto(true, false, principal.getUsername(), principal.getGithubId(), "GITHUB_AUTHENTICATED", false);
            }

            // User is authenticated and matches admin GitHub ID
            boolean verified = isPasswordVerified(request);
            if (verified) {
                return new AdminStatusDto(true, true, principal.getUsername(), principal.getGithubId(), "ADMIN_VERIFIED", false);
            } else {
                return new AdminStatusDto(true, false, principal.getUsername(), principal.getGithubId(), "PASSWORD_REQUIRED", true);
            }
        } catch (UnauthenticatedException ex) {
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
     *
     * @param rawPassword password entered by the user
     * @param request current HTTP request
     * @return updated AdminStatusDto reflecting verified admin status
     */
    public AdminStatusDto verifyAdminPassword(String rawPassword, HttpServletRequest request) {
        // Step 1: Enforce GitHub authentication & admin account identity
        AppUserPrincipal principal = requireAdmin();

        // Step 2: Check attempt lockout (session & IP)
        long lockoutRemaining = adminPasswordRateLimiter.getLockoutRemainingSeconds(request);
        if (lockoutRemaining > 0) {
            log.warn("Admin password verification rejected: temporarily locked out (remaining: {}s)", lockoutRemaining);
            throw new AdminLockoutException(lockoutRemaining);
        }

        // Step 3: Fail closed if ADMIN_PASSWORD_HASH is missing, blank, or invalid
        if (!isValidBcryptHash(adminPasswordHash)) {
            log.warn("Admin password verification attempted by user {} (githubId: {}), but ADMIN_PASSWORD_HASH is not configured or is invalid. Failing closed.",
                    principal.getUsername(), principal.getGithubId());
            recordSecurityAudit(principal, "FAILED_ADMIN_PASSWORD_VERIFICATION", "Password verification attempted while ADMIN_PASSWORD_HASH is not configured");
            throw new AccessDeniedException("Administrative password verification is disabled or unconfigured on this instance");
        }

        // Step 4: Verify password with BCrypt
        if (rawPassword == null || !passwordEncoder.matches(rawPassword, adminPasswordHash)) {
            long lockoutSeconds = adminPasswordRateLimiter.recordFailure(request);
            recordSecurityAudit(principal, "FAILED_ADMIN_PASSWORD_VERIFICATION",
                    "Invalid password attempt" + (lockoutSeconds > 0 ? " (Account locked for 15 minutes)" : ""));
            log.warn("Admin password verification failed for user {} (githubId: {})",
                    principal.getUsername(), principal.getGithubId());

            if (lockoutSeconds > 0) {
                throw new AdminLockoutException(lockoutSeconds);
            }
            throw new InvalidAdminPasswordException("Invalid administrator password");
        }

        // Step 5: Verification success
        adminPasswordRateLimiter.recordSuccess(request);

        // Session fixation protection: rotate session ID while preserving attributes
        if (request != null) {
            request.changeSessionId();
            HttpSession session = request.getSession(true);
            session.setAttribute(SESSION_ATTR_ADMIN_PASSWORD_VERIFIED, Boolean.TRUE);
        }

        log.info("Administrator password successfully verified for user {} (githubId: {})",
                principal.getUsername(), principal.getGithubId());
        recordSecurityAudit(principal, "ADMIN_PASSWORD_VERIFIED", "Administrator password successfully verified");

        return new AdminStatusDto(true, true, principal.getUsername(), principal.getGithubId(), "ADMIN_VERIFIED", false);
    }

    /**
     * Terminates the administrative session, invalidating session state and clearing security context.
     */
    public void logout(HttpServletRequest request) {
        AppUserPrincipal principal = null;
        try {
            principal = currentUserProvider.getCurrentPrincipal();
        } catch (Exception ignored) {
        }

        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.removeAttribute(SESSION_ATTR_ADMIN_PASSWORD_VERIFIED);
                session.invalidate();
            }
        }
        SecurityContextHolder.clearContext();

        if (principal != null) {
            recordSecurityAudit(principal, "ADMIN_LOGOUT", "Admin session logged out and invalidated");
        }
    }

    private boolean isValidBcryptHash(String hash) {
        return hash != null && !hash.isBlank() && BCRYPT_PATTERN.matcher(hash).matches();
    }

    private void recordSecurityAudit(AppUserPrincipal principal, String action, String details) {
        try {
            AdminAuditLog auditLog = new AdminAuditLog(
                    principal.getGithubId(),
                    principal.getUsername(),
                    action,
                    "SECURITY",
                    String.valueOf(principal.getGithubId()),
                    details
            );
            adminAuditLogRepository.save(auditLog);
        } catch (Exception ex) {
            log.error("Failed to persist security audit log for user {}: {}", principal.getUsername(), ex.getMessage());
        }
    }

    public long getAdminGithubId() {
        return adminGithubId;
    }
}
