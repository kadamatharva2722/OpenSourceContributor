package com.osc.backend.discovery.ranking;

import com.osc.backend.github.model.GitHubIssue;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates a GitHub issue with its computed relevance score breakdown and matched factors.
 * Provides deterministic comparator ordering:
 * <ol>
 *   <li>Total score (descending)</li>
 *   <li>Updated timestamp (descending, most recent first)</li>
 *   <li>Issue database ID (descending)</li>
 *   <li>Issue number (descending)</li>
 * </ol>
 */
public class ScoredIssue implements Comparable<ScoredIssue>, Serializable {

    private static final long serialVersionUID = 1L;

    private final GitHubIssue issue;
    private final double totalScore;
    private final double languageScore;
    private final double repositoryScore;
    private final double labelScore;
    private final double keywordScore;
    private final double beginnerScore;
    private final double stateScore;
    private final double freshnessScore;
    private final List<String> matchedFactors;

    public ScoredIssue(GitHubIssue issue, double totalScore, double languageScore,
                       double repositoryScore, double labelScore, double keywordScore,
                       double beginnerScore, double stateScore, double freshnessScore,
                       List<String> matchedFactors) {
        this.issue = issue;
        this.totalScore = totalScore;
        this.languageScore = languageScore;
        this.repositoryScore = repositoryScore;
        this.labelScore = labelScore;
        this.keywordScore = keywordScore;
        this.beginnerScore = beginnerScore;
        this.stateScore = stateScore;
        this.freshnessScore = freshnessScore;
        this.matchedFactors = matchedFactors != null ? Collections.unmodifiableList(new ArrayList<>(matchedFactors)) : Collections.emptyList();
    }

    public GitHubIssue getIssue() {
        return issue;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public double getLanguageScore() {
        return languageScore;
    }

    public double getRepositoryScore() {
        return repositoryScore;
    }

    public double getLabelScore() {
        return labelScore;
    }

    public double getKeywordScore() {
        return keywordScore;
    }

    public double getBeginnerScore() {
        return beginnerScore;
    }

    public double getStateScore() {
        return stateScore;
    }

    public double getFreshnessScore() {
        return freshnessScore;
    }

    public List<String> getMatchedFactors() {
        return matchedFactors;
    }

    @Override
    public int compareTo(ScoredIssue other) {
        if (other == null) {
            return -1;
        }

        // 1. Primary sort: totalScore descending
        int scoreCompare = Double.compare(other.totalScore, this.totalScore);
        if (scoreCompare != 0) {
            return scoreCompare;
        }

        // 2. Secondary sort: updatedAt descending (most recent first)
        Instant thisUpdated = this.issue != null ? this.issue.getUpdatedAt() : null;
        Instant otherUpdated = other.issue != null ? other.issue.getUpdatedAt() : null;
        if (thisUpdated != null && otherUpdated != null) {
            int updatedCompare = otherUpdated.compareTo(thisUpdated);
            if (updatedCompare != 0) {
                return updatedCompare;
            }
        } else if (thisUpdated != null) {
            return -1;
        } else if (otherUpdated != null) {
            return 1;
        }

        // 3. Tertiary sort: issue ID descending (deterministic tie-breaker)
        Long thisId = this.issue != null ? this.issue.getId() : null;
        Long otherId = other.issue != null ? other.issue.getId() : null;
        if (thisId != null && otherId != null) {
            int idCompare = otherId.compareTo(thisId);
            if (idCompare != 0) {
                return idCompare;
            }
        } else if (thisId != null) {
            return -1;
        } else if (otherId != null) {
            return 1;
        }

        // 4. Quaternary sort: issue number descending
        Integer thisNum = this.issue != null ? this.issue.getNumber() : null;
        Integer otherNum = other.issue != null ? other.issue.getNumber() : null;
        if (thisNum != null && otherNum != null) {
            return otherNum.compareTo(thisNum);
        }

        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ScoredIssue that = (ScoredIssue) o;
        return Double.compare(that.totalScore, totalScore) == 0 &&
                Objects.equals(issue, that.issue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(issue, totalScore);
    }

    @Override
    public String toString() {
        return "ScoredIssue{" +
                "issueNumber=" + (issue != null ? issue.getNumber() : "null") +
                ", totalScore=" + totalScore +
                ", languageScore=" + languageScore +
                ", labelScore=" + labelScore +
                ", keywordScore=" + keywordScore +
                ", beginnerScore=" + beginnerScore +
                ", freshnessScore=" + freshnessScore +
                '}';
    }
}
