package com.osc.backend.admin.service;

import com.osc.backend.admin.dto.AdminAuditLogDto;
import com.osc.backend.admin.dto.AdminDashboardStatsDto;
import com.osc.backend.admin.dto.AdminUserDto;
import com.osc.backend.admin.entity.AdminAuditLog;
import com.osc.backend.admin.repository.AdminAuditLogRepository;
import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.common.security.AppUserPrincipal;
import com.osc.backend.contact.dto.ContactResponseDto;
import com.osc.backend.contact.entity.ContactMessage;
import com.osc.backend.contact.repository.ContactMessageRepository;
import com.osc.backend.report.dto.IssueReportResponseDto;
import com.osc.backend.report.entity.IssueReport;
import com.osc.backend.report.repository.IssueReportRepository;
import com.osc.backend.request.dto.RepositoryRequestResponseDto;
import com.osc.backend.request.entity.RepositoryRequest;
import com.osc.backend.request.repository.RepositoryRequestRepository;
import com.osc.backend.user.entity.User;
import com.osc.backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Set;

@Service
public class AdminService {

    private static final Set<String> ALLOWED_MESSAGE_STATUSES = Set.of("UNREAD", "READ", "RESOLVED", "ARCHIVED");
    private static final Set<String> ALLOWED_REPORT_STATUSES = Set.of("PENDING", "REVIEWED", "RESOLVED", "DISMISSED");
    private static final Set<String> ALLOWED_REPO_REQ_STATUSES = Set.of("PENDING", "APPROVED", "REJECTED");

    private final UserRepository userRepository;
    private final ContactMessageRepository contactMessageRepository;
    private final IssueReportRepository issueReportRepository;
    private final RepositoryRequestRepository repositoryRequestRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;

    public AdminService(
            UserRepository userRepository,
            ContactMessageRepository contactMessageRepository,
            IssueReportRepository issueReportRepository,
            RepositoryRequestRepository repositoryRequestRepository,
            AdminAuditLogRepository adminAuditLogRepository
    ) {
        this.userRepository = userRepository;
        this.contactMessageRepository = contactMessageRepository;
        this.issueReportRepository = issueReportRepository;
        this.repositoryRequestRepository = repositoryRequestRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    @Transactional
    public AdminDashboardStatsDto getDashboardStats(AppUserPrincipal admin) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        OffsetDateTime sevenDaysAgo = now.minusDays(7);

        long totalUsers = userRepository.count();
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(startOfMonth);
        long newUsersLast7Days = userRepository.countByCreatedAtAfter(sevenDaysAgo);

        long totalMessages = contactMessageRepository.count();
        long unreadMessages = contactMessageRepository.countByStatus("UNREAD");

        long totalReports = issueReportRepository.count();
        long pendingReports = issueReportRepository.countByStatus("PENDING");

        long totalRepoRequests = repositoryRequestRepository.count();
        long pendingRepoRequests = repositoryRequestRepository.countByStatus("PENDING");

        if (admin != null) {
            recordAuditLog(admin, "VIEW_DASHBOARD", "ADMIN_CONSOLE", "OVERVIEW", "Retrieved system aggregate metrics");
        }

        return new AdminDashboardStatsDto(
                totalUsers,
                newUsersThisMonth,
                newUsersLast7Days,
                totalMessages,
                unreadMessages,
                totalReports,
                pendingReports,
                totalRepoRequests,
                pendingRepoRequests
        );
    }

    @Transactional
    public Page<AdminUserDto> getUsers(String search, Pageable pageable, AppUserPrincipal admin) {
        Page<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userRepository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(
                    search.trim(), search.trim(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }

        if (admin != null) {
            String details = (search != null && !search.isBlank()) ? "Search query: " + search.trim() : "Browsed registered users directory";
            recordAuditLog(admin, "SEARCH_USERS", "USER_DIRECTORY", "PAGE_" + pageable.getPageNumber(), details);
        }

        return users.map(AdminUserDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ContactResponseDto> getMessages(String status, String search, Pageable pageable) {
        return contactMessageRepository.searchMessages(status, search, pageable)
                .map(ContactResponseDto::fromEntity);
    }

    @Transactional
    public ContactResponseDto getMessageById(Long id, AppUserPrincipal admin) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found with id: " + id));

        if (admin != null) {
            recordAuditLog(admin, "VIEW_MESSAGE_PAYLOAD", "CONTACT_MESSAGE", id.toString(), "Viewed message ref: " + message.getReferenceCode());
        }

        return ContactResponseDto.fromEntity(message);
    }

    @Transactional
    public ContactResponseDto updateMessageStatus(Long id, String newStatus, String notes, AppUserPrincipal admin) {
        String upperStatus = newStatus != null ? newStatus.trim().toUpperCase() : "";
        if (!ALLOWED_MESSAGE_STATUSES.contains(upperStatus)) {
            throw new InvalidRequestException("Invalid message status: " + newStatus + ". Allowed: " + ALLOWED_MESSAGE_STATUSES);
        }

        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found with id: " + id));

        String oldStatus = message.getStatus();
        message.setStatus(upperStatus);
        ContactMessage saved = contactMessageRepository.save(message);

        recordAuditLog(
                admin,
                "UPDATE_MESSAGE_STATUS",
                "CONTACT_MESSAGE",
                id.toString(),
                "Changed status from " + oldStatus + " to " + upperStatus + (notes != null ? "; Notes: " + notes : "")
        );

        return ContactResponseDto.fromEntity(saved);
    }

    @Transactional
    public void deleteMessage(Long id, AppUserPrincipal admin) {
        ContactMessage message = contactMessageRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contact message not found with id: " + id));

        contactMessageRepository.delete(message);

        recordAuditLog(
                admin,
                "DELETE_MESSAGE",
                "CONTACT_MESSAGE",
                id.toString(),
                "Deleted message ref: " + message.getReferenceCode() + " from " + message.getEmail()
        );
    }

    @Transactional(readOnly = true)
    public Page<IssueReportResponseDto> getIssueReports(String status, String search, Pageable pageable) {
        return issueReportRepository.searchReports(status, search, pageable)
                .map(IssueReportResponseDto::fromEntity);
    }

    @Transactional
    public IssueReportResponseDto updateIssueReportStatus(Long id, String newStatus, String notes, AppUserPrincipal admin) {
        String upperStatus = newStatus != null ? newStatus.trim().toUpperCase() : "";
        if (!ALLOWED_REPORT_STATUSES.contains(upperStatus)) {
            throw new InvalidRequestException("Invalid report status: " + newStatus + ". Allowed: " + ALLOWED_REPORT_STATUSES);
        }

        IssueReport report = issueReportRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Issue report not found with id: " + id));

        String oldStatus = report.getStatus();
        report.setStatus(upperStatus);
        IssueReport saved = issueReportRepository.save(report);

        recordAuditLog(
                admin,
                "UPDATE_ISSUE_REPORT_STATUS",
                "ISSUE_REPORT",
                id.toString(),
                "Changed status from " + oldStatus + " to " + upperStatus + (notes != null ? "; Notes: " + notes : "")
        );

        return IssueReportResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<RepositoryRequestResponseDto> getRepositoryRequests(String status, String search, Pageable pageable) {
        return repositoryRequestRepository.searchRequests(status, search, pageable)
                .map(RepositoryRequestResponseDto::fromEntity);
    }

    @Transactional
    public RepositoryRequestResponseDto updateRepositoryRequestStatus(Long id, String newStatus, String notes, AppUserPrincipal admin) {
        String upperStatus = newStatus != null ? newStatus.trim().toUpperCase() : "";
        if (!ALLOWED_REPO_REQ_STATUSES.contains(upperStatus)) {
            throw new InvalidRequestException("Invalid repository request status: " + newStatus + ". Allowed: " + ALLOWED_REPO_REQ_STATUSES);
        }

        RepositoryRequest request = repositoryRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Repository request not found with id: " + id));

        String oldStatus = request.getStatus();
        request.setStatus(upperStatus);
        RepositoryRequest saved = repositoryRequestRepository.save(request);

        recordAuditLog(
                admin,
                "UPDATE_REPO_REQUEST_STATUS",
                "REPOSITORY_REQUEST",
                id.toString(),
                "Changed status from " + oldStatus + " to " + upperStatus + (notes != null ? "; Notes: " + notes : "")
        );

        return RepositoryRequestResponseDto.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public Page<AdminAuditLogDto> getAuditLogs(String action, String search, Pageable pageable) {
        return adminAuditLogRepository.searchLogs(action, search, pageable)
                .map(AdminAuditLogDto::fromEntity);
    }

    private void recordAuditLog(AppUserPrincipal admin, String action, String targetType, String targetId, String details) {
        AdminAuditLog log = new AdminAuditLog(
                admin.getGithubId(),
                admin.getUsername(),
                action,
                targetType,
                targetId,
                details
        );
        adminAuditLogRepository.save(log);
    }
}
