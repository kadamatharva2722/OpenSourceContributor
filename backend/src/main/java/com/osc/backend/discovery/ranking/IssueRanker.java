package com.osc.backend.discovery.ranking;

import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.github.model.GitHubIssue;

import java.util.List;

/**
 * Contract for ranking GitHub issues by relevance to user search criteria.
 * Guarantees deterministic ordering, multi-factor scoring, and safe handling of incomplete metadata.
 */
public interface IssueRanker {

    /**
     * Ranks a collection of GitHub issues in descending order of relevance.
     *
     * @param issues raw issues returned from GitHub API
     * @param filter user search criteria and preferences
     * @return sorted list of scored issues (highest relevance first)
     */
    List<ScoredIssue> rankIssues(List<GitHubIssue> issues, ParsedFilterDto filter);

    /**
     * Calculates the multi-factor relevance score and matched criteria for a single issue.
     *
     * @param issue  GitHub issue to score
     * @param filter user search criteria
     * @return ScoredIssue containing score breakdown and matched factors
     */
    ScoredIssue scoreIssue(GitHubIssue issue, ParsedFilterDto filter);
}
