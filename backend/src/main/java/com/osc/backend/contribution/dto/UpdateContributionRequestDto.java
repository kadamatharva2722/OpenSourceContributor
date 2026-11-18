package com.osc.backend.contribution.dto;

import com.osc.backend.contribution.entity.ContributionStatus;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating details of an existing contribution record.
 */
public class UpdateContributionRequestDto {

    private ContributionStatus status;

    @Size(max = 255, message = "Branch name must not exceed 255 characters")
    private String branchName;

    private String pullRequestUrl;

    private String notes;

    public UpdateContributionRequestDto() {
    }

    public UpdateContributionRequestDto(ContributionStatus status, String branchName, String pullRequestUrl, String notes) {
        this.status = status;
        this.branchName = branchName;
        this.pullRequestUrl = pullRequestUrl;
        this.notes = notes;
    }

    public ContributionStatus getStatus() {
        return status;
    }

    public void setStatus(ContributionStatus status) {
        this.status = status;
    }

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getPullRequestUrl() {
        return pullRequestUrl;
    }

    public void setPullRequestUrl(String pullRequestUrl) {
        this.pullRequestUrl = pullRequestUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
