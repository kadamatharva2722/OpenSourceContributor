package com.osc.backend.github.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Domain model representing a GitHub issue.
 */
public class GitHubIssue implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer number;
    private String title;
    private String body;
    private String htmlUrl;
    private String state;
    private String repositoryFullName;
    private List<GitHubLabel> labels = new ArrayList<>();
    private String authorUsername;
    private String authorAvatarUrl;
    private Integer commentsCount;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;

    public GitHubIssue() {
    }

    public GitHubIssue(Long id, Integer number, String title, String body, String htmlUrl,
                       String state, String repositoryFullName, List<GitHubLabel> labels,
                       String authorUsername, String authorAvatarUrl, Integer commentsCount,
                       Instant createdAt, Instant updatedAt, Instant closedAt) {
        this.id = id;
        this.number = number;
        this.title = title;
        this.body = body;
        this.htmlUrl = htmlUrl;
        this.state = state;
        this.repositoryFullName = repositoryFullName;
        this.labels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
        this.authorUsername = authorUsername;
        this.authorAvatarUrl = authorAvatarUrl;
        this.commentsCount = commentsCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.closedAt = closedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRepositoryFullName() {
        return repositoryFullName;
    }

    public void setRepositoryFullName(String repositoryFullName) {
        this.repositoryFullName = repositoryFullName;
    }

    public List<GitHubLabel> getLabels() {
        return labels;
    }

    public void setLabels(List<GitHubLabel> labels) {
        this.labels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
    }

    public String getAuthorUsername() {
        return authorUsername;
    }

    public void setAuthorUsername(String authorUsername) {
        this.authorUsername = authorUsername;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    public Integer getCommentsCount() {
        return commentsCount;
    }

    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubIssue that = (GitHubIssue) o;
        return Objects.equals(id, that.id) && Objects.equals(number, that.number) && Objects.equals(repositoryFullName, that.repositoryFullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, number, repositoryFullName);
    }

    @Override
    public String toString() {
        return "GitHubIssue{" +
                "id=" + id +
                ", number=" + number +
                ", title='" + title + '\'' +
                ", state='" + state + '\'' +
                ", repositoryFullName='" + repositoryFullName + '\'' +
                '}';
    }
}
