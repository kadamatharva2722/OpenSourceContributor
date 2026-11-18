package com.osc.backend.github.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Domain model representing a GitHub repository.
 */
public class GitHubRepository implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String fullName;
    private String description;
    private String htmlUrl;
    private String language;
    private Integer stargazersCount;
    private Integer forksCount;
    private Integer openIssuesCount;
    private List<String> topics = new ArrayList<>();
    private String licenseName;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant pushedAt;

    public GitHubRepository() {
    }

    public GitHubRepository(long id, String name, String fullName, String description,
                            int stargazersCount, int forksCount, String language, String htmlUrl) {
        this(id, name, fullName, description, htmlUrl, language, stargazersCount, forksCount, 0, null, null, null, null, null);
    }

    public GitHubRepository(Long id, String name, String fullName, String description,
                            String htmlUrl, String language, Integer stargazersCount,
                            Integer forksCount, Integer openIssuesCount, List<String> topics,
                            String licenseName, Instant createdAt, Instant updatedAt,
                            Instant pushedAt) {
        this.id = id;
        this.name = name;
        this.fullName = fullName;
        this.description = description;
        this.htmlUrl = htmlUrl;
        this.language = language;
        this.stargazersCount = stargazersCount;
        this.forksCount = forksCount;
        this.openIssuesCount = openIssuesCount;
        this.topics = topics != null ? new ArrayList<>(topics) : new ArrayList<>();
        this.licenseName = licenseName;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.pushedAt = pushedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHtmlUrl() {
        return htmlUrl;
    }

    public void setHtmlUrl(String htmlUrl) {
        this.htmlUrl = htmlUrl;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Integer getStargazersCount() {
        return stargazersCount;
    }

    public void setStargazersCount(Integer stargazersCount) {
        this.stargazersCount = stargazersCount;
    }

    public Integer getForksCount() {
        return forksCount;
    }

    public void setForksCount(Integer forksCount) {
        this.forksCount = forksCount;
    }

    public Integer getOpenIssuesCount() {
        return openIssuesCount;
    }

    public void setOpenIssuesCount(Integer openIssuesCount) {
        this.openIssuesCount = openIssuesCount;
    }

    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics != null ? new ArrayList<>(topics) : new ArrayList<>();
    }

    public String getLicenseName() {
        return licenseName;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
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

    public Instant getPushedAt() {
        return pushedAt;
    }

    public void setPushedAt(Instant pushedAt) {
        this.pushedAt = pushedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubRepository that = (GitHubRepository) o;
        return Objects.equals(id, that.id) && Objects.equals(fullName, that.fullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, fullName);
    }

    @Override
    public String toString() {
        return "GitHubRepository{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", fullName='" + fullName + '\'' +
                ", language='" + language + '\'' +
                ", stargazersCount=" + stargazersCount +
                '}';
    }
}
