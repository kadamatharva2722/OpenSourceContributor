package com.osc.backend.github.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Generic paginated response container for GitHub API resources.
 *
 * @param <T> Domain resource item type
 */
public class GitHubPage<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Maximum accessible search result window per query enforced by the GitHub Search API.
     */
    public static final int GITHUB_MAX_SEARCH_RESULTS = 1000;

    private List<T> items;
    private long totalCount;
    private int page;
    private int perPage;
    private boolean hasNext;
    private boolean hasPrevious;
    private GitHubRateLimit rateLimit;

    public GitHubPage() {
        this.items = new ArrayList<>();
    }

    public GitHubPage(List<T> items, long totalCount, int page, int perPage,
                      boolean hasNext, boolean hasPrevious, GitHubRateLimit rateLimit) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
        this.totalCount = totalCount;
        this.page = page;
        this.perPage = perPage;
        this.hasNext = hasNext;
        this.hasPrevious = hasPrevious;
        this.rateLimit = rateLimit;
    }

    public static <T> GitHubPage<T> empty(int page, int perPage, GitHubRateLimit rateLimit) {
        return new GitHubPage<>(Collections.emptyList(), 0L, page, perPage, false, false, rateLimit);
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<>();
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    /**
     * Calculates the accessible result count capped by GitHub's search window.
     */
    public long getAccessibleCount() {
        return Math.min(Math.max(0L, totalCount), GITHUB_MAX_SEARCH_RESULTS);
    }

    /**
     * Calculates the total accessible pages based on accessibleCount and perPage.
     */
    public int getTotalPages() {
        long accessible = getAccessibleCount();
        if (accessible <= 0 || perPage <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) accessible / perPage);
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

    public GitHubRateLimit getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(GitHubRateLimit rateLimit) {
        this.rateLimit = rateLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubPage<?> that = (GitHubPage<?>) o;
        return totalCount == that.totalCount && page == that.page &&
                perPage == that.perPage && hasNext == that.hasNext &&
                hasPrevious == that.hasPrevious && Objects.equals(items, that.items) &&
                Objects.equals(rateLimit, that.rateLimit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(items, totalCount, page, perPage, hasNext, hasPrevious, rateLimit);
    }

    @Override
    public String toString() {
        return "GitHubPage{" +
                "itemsCount=" + (items != null ? items.size() : 0) +
                ", totalCount=" + totalCount +
                ", page=" + page +
                ", perPage=" + perPage +
                ", hasNext=" + hasNext +
                ", hasPrevious=" + hasPrevious +
                '}';
    }
}
