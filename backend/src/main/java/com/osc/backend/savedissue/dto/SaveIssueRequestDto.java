package com.osc.backend.savedissue.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for saving/bookmarking a GitHub issue.
 */
public class SaveIssueRequestDto {

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

    @NotBlank(message = "Issue URL is required")
    private String issueUrl;

    public SaveIssueRequestDto() {
    }

    public SaveIssueRequestDto(Long githubIssueId, String repositoryFullName, Integer issueNumber, String title, String issueUrl) {
        this.githubIssueId = githubIssueId;
        this.repositoryFullName = repositoryFullName;
        this.issueNumber = issueNumber;
        this.title = title;
        this.issueUrl = issueUrl;
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

    public String getIssueUrl() {
        return issueUrl;
    }

    public void setIssueUrl(String issueUrl) {
        this.issueUrl = issueUrl;
    }
}
