package com.osc.backend.admin.security;

import com.osc.backend.admin.dto.AdminStatusDto;
import com.osc.backend.admin.entity.AdminAuditLog;
import com.osc.backend.admin.repository.AdminAuditLogRepository;
import com.osc.backend.common.exception.UnauthenticatedException;
import com.osc.backend.common.security.AppUserPrincipal;
import com.osc.backend.common.security.CurrentUserProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Enforces admin authorization by verifying the permanent, immutable GitHub numeric ID
 * of the authenticated user against the configured APP_ADMIN_GITHUB_ID.
 */
@Service
public class AdminSecurityService {

    private static final Logger log = LoggerFactory.getLogger(AdminSecurityService.class);

    private final CurrentUserProvider currentUserProvider;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final long adminGithubId;

    public AdminSecurityService(
            CurrentUserProvider currentUserProvider,
            AdminAuditLogRepository adminAuditLogRepository,
            @Value("${app.security.admin.github-id:0}") long adminGithubId
    ) {
        this.currentUserProvider = currentUserProvider;
        this.adminAuditLogRepository = adminAuditLogRepository;
        this.adminGithubId = adminGithubId;
    }

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

    public AdminStatusDto getAdminStatus() {
        try {
            AppUserPrincipal principal = currentUserProvider.getCurrentPrincipal();
            if (principal == null) {
                return new AdminStatusDto(false, false, null, null);
            }
            boolean isAdm = adminGithubId > 0 && Objects.equals(principal.getGithubId(), adminGithubId);
            return new AdminStatusDto(true, isAdm, principal.getUsername(), principal.getGithubId());
        } catch (UnauthenticatedException ex) {
            return new AdminStatusDto(false, false, null, null);
        }
    }

    public long getAdminGithubId() {
        return adminGithubId;
    }
}
