package com.osc.backend.admin.controller;

import com.osc.backend.admin.dto.AdminAuditLogDto;
import com.osc.backend.admin.dto.AdminDashboardStatsDto;
import com.osc.backend.admin.dto.AdminPasswordVerifyRequestDto;
import com.osc.backend.admin.dto.AdminStatusDto;
import com.osc.backend.admin.dto.AdminUserDto;
import com.osc.backend.admin.dto.StatusUpdateDto;
import com.osc.backend.admin.security.AdminSecurityService;
import com.osc.backend.admin.service.AdminService;
import com.osc.backend.common.security.AppUserPrincipal;
import com.osc.backend.contact.dto.ContactResponseDto;
import com.osc.backend.report.dto.IssueReportResponseDto;
import com.osc.backend.request.dto.RepositoryRequestResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing administrative REST endpoints.
 *
 * <p>Access requires server-side administrator password verification against BCrypt {@code ADMIN_PASSWORD_HASH}.
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminSecurityService adminSecurityService;
    private final AdminService adminService;

    public AdminController(AdminSecurityService adminSecurityService, AdminService adminService) {
        this.adminSecurityService = adminSecurityService;
        this.adminService = adminService;
    }

    /**
     * Retrieves the current multi-step authentication status of the caller.
     */
    @GetMapping("/auth/status")
    public ResponseEntity<AdminStatusDto> getAdminAuthStatus(HttpServletRequest request) {
        return ResponseEntity.ok(adminSecurityService.getAdminStatus(request));
    }

    /**
     * Verifies the administrator password (factor 2).
     * Upon success, sets server-side session authorization and rotates session ID.
     */
    @PostMapping("/auth/verify-password")
    public ResponseEntity<AdminStatusDto> verifyAdminPassword(
            @Valid @RequestBody AdminPasswordVerifyRequestDto requestDto,
            HttpServletRequest request
    ) {
        AdminStatusDto status = adminSecurityService.verifyAdminPassword(requestDto.getPassword(), request);
        return ResponseEntity.ok(status);
    }

    /**
     * Logs out the administrator session, destroying session state and clearing security context.
     */
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        adminSecurityService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/dashboard")
    public ResponseEntity<AdminDashboardStatsDto> getDashboardStats(HttpServletRequest request) {
        AppUserPrincipal admin = adminSecurityService.requireAdminVerified(request);
        return ResponseEntity.ok(adminService.getDashboardStats(admin));
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDto>> getUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request
    ) {
        AppUserPrincipal admin = adminSecurityService.requireAdminVerified(request);
        return ResponseEntity.ok(adminService.getUsers(search, pageable, admin));
    }

    @GetMapping("/messages")
    public ResponseEntity<Page<ContactResponseDto>> getMessages(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request
    ) {
        adminSecurityService.requireAdminVerified(request);
        return ResponseEntity.ok(adminService.getMessages(status, search, pageable));
    }

    @GetMapping("/messages/{id}")
    public ResponseEntity<ContactResponseDto> getMessageById(@PathVariable Long id, HttpServletRequest request) {
        AppUserPrincipal admin = adminSecurityService.requireAdminVerified(request);
        return ResponseEntity.ok(adminService.getMessageById(id, admin));
    }

    @PatchMapping("/messages/{id}/status")
    public ResponseEntity<ContactResponseDto> updateMessageStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateDto updateDto,
            HttpServletRequest request
    ) {
        AppUserPrincipal admin = adminSecurityService.requireAdminVerified(request);
        ContactResponseDto updated = adminService.updateMessageStatus(id, updateDto.getStatus(), updateDto.getNotes(), admin);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id, HttpServletRequest request) {
        AppUserPrincipal admin = adminSecurityService.requireAdminVerified(request);
        adminService.deleteMessage(id, admin);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/issue-reports")
    public ResponseEntity<Page<IssueReportResponseDto>> getIssueReports(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request
    ) {
        adminSecurityService.requireAdminVerified(request);
        return ResponseEntity.ok(adminService.getIssueReports(status, search, pageable));
    }

    @PatchMapping("/issue-reports/{id}/status")
    public ResponseEntity<IssueReportResponseDto> updateIssueReportStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateDto updateDto,
            HttpServletRequest request
    ) {
        AppUserPrincipal admin = adminSecurityService.requireAdminVerified(request);
        IssueReportResponseDto updated = adminService.updateIssueReportStatus(id, updateDto.getStatus(), updateDto.getNotes(), admin);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/repository-requests")
    public ResponseEntity<Page<RepositoryRequestResponseDto>> getRepositoryRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request
    ) {
        adminSecurityService.requireAdminVerified(request);
        return ResponseEntity.ok(adminService.getRepositoryRequests(status, search, pageable));
    }

    @PatchMapping("/repository-requests/{id}/status")
    public ResponseEntity<RepositoryRequestResponseDto> updateRepositoryRequestStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateDto updateDto,
            HttpServletRequest request
    ) {
        AppUserPrincipal admin = adminSecurityService.requireAdminVerified(request);
        RepositoryRequestResponseDto updated = adminService.updateRepositoryRequestStatus(id, updateDto.getStatus(), updateDto.getNotes(), admin);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/audit-logs")
    public ResponseEntity<Page<AdminAuditLogDto>> getAuditLogs(
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            HttpServletRequest request
    ) {
        adminSecurityService.requireAdminVerified(request);
        return ResponseEntity.ok(adminService.getAuditLogs(action, search, pageable));
    }
}
