package com.osc.backend.github.dto.upstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

/**
 * Raw upstream GitHub API issue representation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubIssueUpstreamDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private Long id;

    @JsonProperty("number")
    private Integer number;

    @JsonProperty("title")
    private String title;

    @JsonProperty("body")
    private String body;

    @JsonProperty("html_url")
    private String htmlUrl;

    @JsonProperty("state")
    private String state;

    @JsonProperty("repository_url")
    private String repositoryUrl;

    @JsonProperty("labels")
    private List<GitHubLabelUpstreamDto> labels;

    @JsonProperty("user")
    private GitHubUserUpstreamDto user;

    @JsonProperty("comments")
    private Integer comments;

    @JsonProperty("pull_request")
    private Object pullRequest;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

    @JsonProperty("closed_at")
    private Instant closedAt;

    public GitHubIssueUpstreamDto() {
    }

    public boolean isPullRequest() {
        return pullRequest != null;
    }

    /**
     * Extracts "owner/repo" from repositoryUrl (e.g. https://api.github.com/repos/owner/repo)
     * or fallback to htmlUrl (e.g. https://github.com/owner/repo/issues/123).
     */
    public String extractRepositoryFullName() {
        if (repositoryUrl != null && !repositoryUrl.isEmpty()) {
            String prefix = "/repos/";
            int idx = repositoryUrl.indexOf(prefix);
            if (idx != -1) {
                return repositoryUrl.substring(idx + prefix.length());
            }
        }
        if (htmlUrl != null && !htmlUrl.isEmpty()) {
            // Format: https://github.com/{owner}/{repo}/issues/{number}
            try {
                String[] parts = htmlUrl.replace("https://github.com/", "").split("/");
                if (parts.length >= 2) {
                    return parts[0] + "/" + parts[1];
                }
            } catch (Exception ignored) {
            }
        }
        return null;
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

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public List<GitHubLabelUpstreamDto> getLabels() {
        return labels;
    }

    public void setLabels(List<GitHubLabelUpstreamDto> labels) {
        this.labels = labels;
    }

    public GitHubUserUpstreamDto getUser() {
        return user;
    }

    public void setUser(GitHubUserUpstreamDto user) {
        this.user = user;
    }

    public Integer getComments() {
        return comments;
    }

    public void setComments(Integer comments) {
        this.comments = comments;
    }

    public Object getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(Object pullRequest) {
        this.pullRequest = pullRequest;
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
}
