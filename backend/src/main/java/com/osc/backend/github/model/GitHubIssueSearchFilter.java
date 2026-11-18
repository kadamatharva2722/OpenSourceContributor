package com.osc.backend.github.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Filter criteria for searching GitHub issues via the public REST API.
 * Encapsulates search keywords, labels, language, repository, and pagination parameters.
 */
public class GitHubIssueSearchFilter implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PER_PAGE = 30;
    public static final int MAX_PER_PAGE = 100;

    private String query;
    private List<String> labels = new ArrayList<>();
    private String language;
    private String state = "open";
    private String repository;
    private String sort;
    private String order;
    private int page = DEFAULT_PAGE;
    private int perPage = DEFAULT_PER_PAGE;

    public GitHubIssueSearchFilter() {
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds the GitHub search query string ('q' parameter).
     * Always ensures 'is:issue' is present to target issues, and 'is:open' by default.
     */
    public String toQueryString() {
        StringBuilder sb = new StringBuilder();
        sb.append("is:issue");

        if (state != null && !state.trim().isEmpty()) {
            sb.append(" is:").append(state.trim().toLowerCase());
        }

        if (repository != null && !repository.trim().isEmpty()) {
            sb.append(" repo:").append(repository.trim());
        }

        if (language != null && !language.trim().isEmpty()) {
            String cleanLang = language.trim();
            if (cleanLang.contains(" ")) {
                sb.append(" language:\"").append(cleanLang).append("\"");
            } else {
                sb.append(" language:").append(cleanLang);
            }
        }

        if (labels != null) {
            for (String label : labels) {
                if (label != null && !label.trim().isEmpty()) {
                    String cleanLabel = label.trim();
                    if (cleanLabel.contains(" ") || cleanLabel.contains(":")) {
                        sb.append(" label:\"").append(cleanLabel).append("\"");
                    } else {
                        sb.append(" label:").append(cleanLabel);
                    }
                }
            }
        }

        if (query != null && !query.trim().isEmpty()) {
            sb.append(" ").append(query.trim());
        }

        return sb.toString().trim();
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getSort() {
        return sort;
    }

    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(page, 1);
    }

    public int getPerPage() {
        return perPage;
    }

    public void setPerPage(int perPage) {
        if (perPage < 1) {
            this.perPage = DEFAULT_PER_PAGE;
        } else {
            this.perPage = Math.min(perPage, MAX_PER_PAGE);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GitHubIssueSearchFilter that = (GitHubIssueSearchFilter) o;
        return page == that.page && perPage == that.perPage &&
                Objects.equals(query, that.query) &&
                Objects.equals(labels, that.labels) &&
                Objects.equals(language, that.language) &&
                Objects.equals(state, that.state) &&
                Objects.equals(repository, that.repository) &&
                Objects.equals(sort, that.sort) &&
                Objects.equals(order, that.order);
    }

    @Override
    public int hashCode() {
        return Objects.hash(query, labels, language, state, repository, sort, order, page, perPage);
    }

    @Override
    public String toString() {
        return "GitHubIssueSearchFilter{" +
                "q='" + toQueryString() + '\'' +
                ", page=" + page +
                ", perPage=" + perPage +
                ", sort='" + sort + '\'' +
                ", order='" + order + '\'' +
                '}';
    }

    public static class Builder {
        private final GitHubIssueSearchFilter filter = new GitHubIssueSearchFilter();

        public Builder query(String query) {
            filter.setQuery(query);
            return this;
        }

        public Builder label(String label) {
            filter.labels.add(label);
            return this;
        }

        public Builder labels(List<String> labels) {
            filter.setLabels(labels);
            return this;
        }

        public Builder language(String language) {
            filter.setLanguage(language);
            return this;
        }

        public Builder state(String state) {
            filter.setState(state);
            return this;
        }

        public Builder repository(String repository) {
            filter.setRepository(repository);
            return this;
        }

        public Builder sort(String sort) {
            filter.setSort(sort);
            return this;
        }

        public Builder order(String order) {
            filter.setOrder(order);
            return this;
        }

        public Builder page(int page) {
            filter.setPage(page);
            return this;
        }

        public Builder perPage(int perPage) {
            filter.setPerPage(perPage);
            return this;
        }

        public GitHubIssueSearchFilter build() {
            return filter;
        }
    }
}
