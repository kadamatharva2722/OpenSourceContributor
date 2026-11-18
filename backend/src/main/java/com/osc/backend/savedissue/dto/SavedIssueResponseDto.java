package com.osc.backend.savedissue.dto;

import com.osc.backend.savedissue.entity.SavedIssue;

import java.time.OffsetDateTime;

/**
 * Data transfer object representing a saved/bookmarked issue.
 */
public class SavedIssueResponseDto {

    private Long id;
    private Long userId;
    private Long githubIssueId;
    private String repositoryFullName;
    private Integer issueNumber;
    private String title;
    private String issueUrl;
    private OffsetDateTime savedAt;

    public SavedIssueResponseDto() {
    }

    public SavedIssueResponseDto(Long id, Long userId, Long githubIssueId, String repositoryFullName,
                                 Integer issueNumber, String title, String issueUrl, OffsetDateTime savedAt) {
        this.id = id;
        this.userId = userId;
        this.githubIssueId = githubIssueId;
        this.repositoryFullName = repositoryFullName;
        this.issueNumber = issueNumber;
        this.title = title;
        this.issueUrl = issueUrl;
        this.savedAt = savedAt;
    }

    public static SavedIssueResponseDto fromEntity(SavedIssue savedIssue) {
        if (savedIssue == null) {
            return null;
        }
        return new SavedIssueResponseDto(
                savedIssue.getId(),
                savedIssue.getUser() != null ? savedIssue.getUser().getId() : null,
                savedIssue.getGithubIssueId(),
                savedIssue.getRepositoryFullName(),
                savedIssue.getIssueNumber(),
                savedIssue.getTitle(),
                savedIssue.getIssueUrl(),
                savedIssue.getSavedAt()
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

    public String getIssueUrl() {
        return issueUrl;
    }

    public void setIssueUrl(String issueUrl) {
        this.issueUrl = issueUrl;
    }

    public OffsetDateTime getSavedAt() {
        return savedAt;
    }

    public void setSavedAt(OffsetDateTime savedAt) {
        this.savedAt = savedAt;
    }
}
