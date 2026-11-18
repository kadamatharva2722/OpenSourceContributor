package com.osc.backend.savedissue.entity;

import com.osc.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

/**
 * JPA entity representing a bookmarked GitHub issue, mapped to "saved_issues".
 */
@Entity
@Table(
    name = "saved_issues",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_saved_issues_user_issue", columnNames = {"user_id", "github_issue_id"})
    }
)
public class SavedIssue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "github_issue_id", nullable = false)
    private Long githubIssueId;

    @Column(name = "repository_full_name", nullable = false, length = 255)
    private String repositoryFullName;

    @Column(name = "issue_number", nullable = false)
    private Integer issueNumber;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "issue_url", nullable = false, columnDefinition = "TEXT")
    private String issueUrl;

    @CreationTimestamp
    @Column(name = "saved_at", nullable = false, updatable = false)
    private OffsetDateTime savedAt;

    public SavedIssue() {
    }

    public SavedIssue(User user, Long githubIssueId, String repositoryFullName, Integer issueNumber, String title, String issueUrl) {
        this.user = user;
        this.githubIssueId = githubIssueId;
        this.repositoryFullName = repositoryFullName;
        this.issueNumber = issueNumber;
        this.title = title;
        this.issueUrl = issueUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    @Override
    public String toString() {
        return "SavedIssue{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", githubIssueId=" + githubIssueId +
                ", repositoryFullName='" + repositoryFullName + '\'' +
                ", issueNumber=" + issueNumber +
                ", title='" + title + '\'' +
                ", issueUrl='" + issueUrl + '\'' +
                ", savedAt=" + savedAt +
                '}';
    }
}
