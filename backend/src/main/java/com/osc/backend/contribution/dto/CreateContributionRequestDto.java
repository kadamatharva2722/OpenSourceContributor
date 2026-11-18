package com.osc.backend.contribution.dto;

import com.osc.backend.contribution.entity.ContributionStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating a new contribution tracker record.
 */
public class CreateContributionRequestDto {

    @NotNull(message = "GitHub issue ID is required")
    private Long githubIssueId;

    @NotBlank(message = "Repository full name is required")
    @Size(max = 255, message = "Repository full name must not exceed 255 characters")
    private String repositoryFullName;

    @NotNull(message = "Issue number is required")
    @Min(value = 1, message = "Issue number must be a positive integer")
    private Integer issueNumber;

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;

    private ContributionStatus status = ContributionStatus.PLANNING;

    @Size(max = 255, message = "Branch name must not exceed 255 characters")
    private String branchName;

    private String pullRequestUrl;

    private String notes;

    public CreateContributionRequestDto() {
    }

    public CreateContributionRequestDto(Long githubIssueId, String repositoryFullName, Integer issueNumber,
                                        String title, ContributionStatus status, String branchName,
                                        String pullRequestUrl, String notes) {
        this.githubIssueId = githubIssueId;
        this.repositoryFullName = repositoryFullName;
        this.issueNumber = issueNumber;
        this.title = title;
        this.status = status != null ? status : ContributionStatus.PLANNING;
        this.branchName = branchName;
        this.pullRequestUrl = pullRequestUrl;
        this.notes = notes;
    }

    public Long getGithubIssueId() {
        return githubIssueId;
    }

    public void setGithubIssueId(Long githubIssueId) {
        this.githubIssueId = githubIssueId;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public void setRepositoryFullName(String repositoryFullName) {
        this.repositoryFullName = repositoryFullName;
    }

    public Integer getIssueNumber() {
        return issueNumber;
    }

    public void setIssueNumber(Integer issueNumber) {
        this.issueNumber = issueNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public ContributionStatus getStatus() {
        return status;
    }

    public void setStatus(ContributionStatus status) {
        this.status = status != null ? status : ContributionStatus.PLANNING;
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
