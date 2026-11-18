package com.osc.backend.discovery.dto;

import com.osc.backend.github.model.GitHubIssue;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * An individual GitHub issue match with factual explanation.
 */
@Schema(description = "Matched GitHub issue with verified factual explanation")
public class IssueMatchDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Domain GitHub issue details")
    private GitHubIssue issue;

    @Schema(description = "Factual match explanation based on actual GitHub issue and repository data",
            example = "Matches requested language: Java; Contains requested label: 'good first issue'")
    private String matchExplanation;

    @Schema(description = "List of concrete criteria successfully matched")
    private List<String> matchedCriteria = new ArrayList<>();

    @Schema(description = "Internal search relevance score (not an objective measure of contribution difficulty or success)", example = "85.0")
    private Double matchScore;

    public IssueMatchDto() {
    }

    public IssueMatchDto(GitHubIssue issue, String matchExplanation, List<String> matchedCriteria) {
        this(issue, matchExplanation, matchedCriteria, null);
    }

    public IssueMatchDto(GitHubIssue issue, String matchExplanation, List<String> matchedCriteria, Double matchScore) {
        this.issue = issue;
        this.matchExplanation = matchExplanation;
        this.matchedCriteria = matchedCriteria != null ? new ArrayList<>(matchedCriteria) : new ArrayList<>();
        this.matchScore = matchScore;
    }

    public Double getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Double matchScore) {
        this.matchScore = matchScore;
    }

    public GitHubIssue getIssue() {
        return issue;
    }

    public void setIssue(GitHubIssue issue) {
        this.issue = issue;
    }

    public String getMatchExplanation() {
        return matchExplanation;
    }

    public void setMatchExplanation(String matchExplanation) {
        this.matchExplanation = matchExplanation;
    }

    public List<String> getMatchedCriteria() {
        return matchedCriteria;
    }

    public void setMatchedCriteria(List<String> matchedCriteria) {
        this.matchedCriteria = matchedCriteria != null ? new ArrayList<>(matchedCriteria) : new ArrayList<>();
    }
}
