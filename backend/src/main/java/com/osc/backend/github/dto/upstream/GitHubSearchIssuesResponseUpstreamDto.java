package com.osc.backend.github.dto.upstream;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Raw upstream GitHub API search issues response representation.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class GitHubSearchIssuesResponseUpstreamDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("total_count")
    private Long totalCount;

    @JsonProperty("incomplete_results")
    private Boolean incompleteResults;

    @JsonProperty("items")
    private List<GitHubIssueUpstreamDto> items = new ArrayList<>();

    public GitHubSearchIssuesResponseUpstreamDto() {
    }

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Boolean getIncompleteResults() {
        return incompleteResults;
    }

    public void setIncompleteResults(Boolean incompleteResults) {
        this.incompleteResults = incompleteResults;
    }

    public List<GitHubIssueUpstreamDto> getItems() {
        return items;
    }

    public void setItems(List<GitHubIssueUpstreamDto> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
