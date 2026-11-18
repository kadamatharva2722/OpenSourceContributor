package com.osc.backend.discovery.dto;

import com.osc.backend.github.model.GitHubRateLimit;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete response payload for GitHub issue discovery (both AI-powered and public guest).
 */
@Schema(description = "Discovery search response containing matched issues, criteria, pagination, and quota")
public class DiscoverySearchResponseDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Discovery mode: GUEST or AUTHENTICATED_AI", example = "AUTHENTICATED_AI")
    private String discoveryMode = "AUTHENTICATED_AI";

    @Schema(description = "Original natural language user prompt (null for guest mode)", example = "Find beginner-friendly Java issues")
    private String prompt;

    @Schema(description = "Validated search criteria derived from prompt or request parameters")
    private ParsedFilterDto parsedFilter;

    @Schema(description = "List of matched issues with factual explanations")
    private List<IssueMatchDto> results = new ArrayList<>();

    @Schema(description = "Total number of matching issues found on GitHub", example = "125")
    private long totalCount;

    @Schema(description = "Total number of accessible issues within GitHub Search API 1,000 result window", example = "125")
    private long accessibleCount;

    @Schema(description = "Total number of accessible pages based on accessibleCount and perPage", example = "5")
    private int totalPages;

    @Schema(description = "Current page number (1-based)", example = "1")
    private int page;

    @Schema(description = "Page size", example = "30")
    private int perPage;

    @Schema(description = "Whether there is a next page available", example = "true")
    private boolean hasNext;

    @Schema(description = "Whether there is a previous page available", example = "false")
    private boolean hasPrevious;

    @Schema(description = "Remaining AI searches available to the user for today (null for guest mode)", example = "19")
    private Integer remainingDailyQuota;

    @Schema(description = "Current GitHub API rate limit state")
    private GitHubRateLimit rateLimit;

    public DiscoverySearchResponseDto() {
    }

    public DiscoverySearchResponseDto(String prompt, ParsedFilterDto parsedFilter, List<IssueMatchDto> results,
                                      long totalCount, int page, int perPage, boolean hasNext,
                                      boolean hasPrevious, Integer remainingDailyQuota, GitHubRateLimit rateLimit) {
        this(prompt, parsedFilter, results, totalCount, page, perPage, hasNext, hasPrevious, remainingDailyQuota, rateLimit, "AUTHENTICATED_AI");
    }

    public DiscoverySearchResponseDto(String prompt, ParsedFilterDto parsedFilter, List<IssueMatchDto> results,
                                      long totalCount, int page, int perPage, boolean hasNext,
                                      boolean hasPrevious, Integer remainingDailyQuota, GitHubRateLimit rateLimit,
                                      String discoveryMode) {
        this(
                prompt,
                parsedFilter,
                results,
                totalCount,
                Math.min(Math.max(0L, totalCount), 1000L),
                (Math.min(Math.max(0L, totalCount), 1000L) > 0 && perPage > 0)
                        ? (int) Math.ceil((double) Math.min(Math.max(0L, totalCount), 1000L) / perPage)
                        : 0,
                page,
                perPage,
                hasNext,
                hasPrevious,
                remainingDailyQuota,
                rateLimit,
                discoveryMode
        );
    }

    public DiscoverySearchResponseDto(String prompt, ParsedFilterDto parsedFilter, List<IssueMatchDto> results,
                                      long totalCount, long accessibleCount, int totalPages,
                                      int page, int perPage, boolean hasNext,
                                      boolean hasPrevious, Integer remainingDailyQuota, GitHubRateLimit rateLimit,
                                      String discoveryMode) {
        this.prompt = prompt;
        this.parsedFilter = parsedFilter;
        this.results = results != null ? new ArrayList<>(results) : new ArrayList<>();
        this.totalCount = totalCount;
        this.accessibleCount = accessibleCount;
        this.totalPages = totalPages;
        this.page = page;
        this.perPage = perPage;
        this.hasNext = page < totalPages && hasNext;
        this.hasPrevious = page > 1 && hasPrevious;
        this.remainingDailyQuota = remainingDailyQuota;
        this.rateLimit = rateLimit;
        this.discoveryMode = discoveryMode != null ? discoveryMode : "AUTHENTICATED_AI";
    }

    public String getDiscoveryMode() {
        return discoveryMode;
    }

    public void setDiscoveryMode(String discoveryMode) {
        this.discoveryMode = discoveryMode;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public ParsedFilterDto getParsedFilter() {
        return parsedFilter;
    }

    public void setParsedFilter(ParsedFilterDto parsedFilter) {
        this.parsedFilter = parsedFilter;
    }

    public List<IssueMatchDto> getResults() {
        return results;
    }

    public void setResults(List<IssueMatchDto> results) {
        this.results = results != null ? new ArrayList<>(results) : new ArrayList<>();
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public long getAccessibleCount() {
        return accessibleCount;
    }

    public void setAccessibleCount(long accessibleCount) {
        this.accessibleCount = accessibleCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPerPage() {
        return perPage;
    }

    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    public boolean isHasNext() {
        return hasNext;
    }

    public void setHasNext(boolean hasNext) {
        this.hasNext = hasNext;
    }

    public boolean isHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(boolean hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public Integer getRemainingDailyQuota() {
        return remainingDailyQuota;
    }

    public void setRemainingDailyQuota(Integer remainingDailyQuota) {
        this.remainingDailyQuota = remainingDailyQuota;
    }

    public GitHubRateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(GitHubRateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }
}
