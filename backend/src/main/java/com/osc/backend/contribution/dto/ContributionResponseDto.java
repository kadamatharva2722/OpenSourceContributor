package com.osc.backend.contribution.dto;

import com.osc.backend.contribution.entity.Contribution;
import com.osc.backend.contribution.entity.ContributionStatus;

import java.time.OffsetDateTime;

/**
 * Data transfer object representing a contribution record.
 */
public class ContributionResponseDto {

    private Long id;
    private Long userId;
    private Long githubIssueId;
    private String repositoryFullName;
    private Integer issueNumber;
    private String title;
    private ContributionStatus status;
    private String branchName;
    private String pullRequestUrl;
    private String notes;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public ContributionResponseDto() {
    }

    public ContributionResponseDto(Long id, Long userId, Long githubIssueId, String repositoryFullName,
                                   Integer issueNumber, String title, ContributionStatus status,
                                   String branchName, String pullRequestUrl, String notes,
                                   OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.githubIssueId = githubIssueId;
        this.repositoryFullName = repositoryFullName;
        this.issueNumber = issueNumber;
        this.title = title;
        this.status = status;
        this.branchName = branchName;
        this.pullRequestUrl = pullRequestUrl;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ContributionResponseDto fromEntity(Contribution contribution) {
        if (contribution == null) {
            return null;
        }
        return new ContributionResponseDto(
                contribution.getId(),
                contribution.getUser() != null ? contribution.getUser().getId() : null,
                contribution.getGithubIssueId(),
                contribution.getRepositoryFullName(),
                contribution.getIssueNumber(),
                contribution.getTitle(),
                contribution.getStatus(),
                contribution.getBranchName(),
                contribution.getPullRequestUrl(),
                contribution.getNotes(),
                contribution.getCreatedAt(),
                contribution.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
