package com.osc.backend.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class IssueReportRequestDto {

    @NotBlank(message = "Repository name is required")
    @Size(max = 255, message = "Repository name must not exceed 255 characters")
    private String repositoryName;

    @NotBlank(message = "Issue URL is required")
    private String issueUrl;

    private Integer issueNumber;

    @NotBlank(message = "Reason is required")
    @Size(max = 50, message = "Reason must not exceed 50 characters")
    private String reason;

    @Size(max = 2000, message = "Details must not exceed 2000 characters")
    private String details;

    @Size(max = 50, message = "Username must not exceed 50 characters")
    private String reportedByUsername;

    public IssueReportRequestDto() {
    }

    public IssueReportRequestDto(String repositoryName, String issueUrl, Integer issueNumber, String reason, String details, String reportedByUsername) {
        this.repositoryName = repositoryName;
        this.issueUrl = issueUrl;
        this.issueNumber = issueNumber;
        this.reason = reason;
        this.details = details;
        this.reportedByUsername = reportedByUsername;
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
}
