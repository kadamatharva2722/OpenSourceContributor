package com.osc.backend.report.dto;

import com.osc.backend.report.entity.IssueReport;

import java.time.OffsetDateTime;

public class IssueReportResponseDto {

    private Long id;
    private String repositoryName;
    private String issueUrl;
    private Integer issueNumber;
    private String reason;
    private String details;
    private String reportedByUsername;
    private String status;
    private OffsetDateTime createdAt;

    public IssueReportResponseDto() {
    }

    public IssueReportResponseDto(Long id, String repositoryName, String issueUrl, Integer issueNumber,
                                  String reason, String details, String reportedByUsername, String status,
                                  OffsetDateTime createdAt) {
        this.id = id;
        this.repositoryName = repositoryName;
        this.issueUrl = issueUrl;
        this.issueNumber = issueNumber;
        this.reason = reason;
        this.details = details;
        this.reportedByUsername = reportedByUsername;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static IssueReportResponseDto fromEntity(IssueReport entity) {
        return new IssueReportResponseDto(
                entity.getId(),
                entity.getRepositoryName(),
                entity.getIssueUrl(),
                entity.getIssueNumber(),
                entity.getReason(),
                entity.getDetails(),
                entity.getReportedByUsername(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getIssueUrl() {
        return issueUrl;
    }

    public void setIssueUrl(String issueUrl) {
        this.issueUrl = issueUrl;
    }

    public Integer getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(Integer issueNumber) {
        this.issueNumber = issueNumber;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getReportedByUsername() {
        return reportedByUsername;
    }

    public void setReportedByUsername(String reportedByUsername) {
        this.reportedByUsername = reportedByUsername;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
