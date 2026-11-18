package com.osc.backend.admin.dto;

import com.osc.backend.admin.entity.AdminAuditLog;

import java.time.OffsetDateTime;

public class AdminAuditLogDto {

    private Long id;
    private Long adminGithubId;
    private String adminUsername;
    private String action;
    private String targetType;
    private String targetId;
    private String details;
    private OffsetDateTime createdAt;

    public AdminAuditLogDto() {
    }

    public AdminAuditLogDto(Long id, Long adminGithubId, String adminUsername, String action, String targetType, String targetId, String details, OffsetDateTime createdAt) {
        this.id = id;
        this.adminGithubId = adminGithubId;
        this.adminUsername = adminUsername;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.createdAt = createdAt;
    }

    public static AdminAuditLogDto fromEntity(AdminAuditLog log) {
        return new AdminAuditLogDto(
                log.getId(),
                log.getAdminGithubId(),
                log.getAdminUsername(),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getDetails(),
                log.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAdminGithubId() {
        return adminGithubId;
    }

    public void setAdminGithubId(Long adminGithubId) {
        this.adminGithubId = adminGithubId;
    }

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
