package com.osc.backend.discovery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Parameters for public anonymous GitHub issue discovery.
 * Requires at least one meaningful search filter to prevent unrestricted broad queries.
 */
@Schema(description = "Parameters for public anonymous GitHub issue browsing")
public class PublicDiscoveryRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Size(max = 100, message = "Keywords query cannot exceed 100 characters")
    @Schema(description = "Keywords to match in issue title and body", example = "memory leak")
    private String query;

    @Size(max = 30, message = "Language cannot exceed 30 characters")
    @Schema(description = "Programming language filter", example = "Java")
    private String language;

    @Size(max = 100, message = "Repository cannot exceed 100 characters")
    @Schema(description = "Repository full name in owner/repo format", example = "spring-projects/spring-boot")
    private String repository;

    @Schema(description = "List of GitHub labels to filter by", example = "[\"good first issue\"]")
    private List<String> labels = new ArrayList<>();

    @Schema(description = "Issue state (open or closed)", example = "open", defaultValue = "open")
    private String state = "open";

    @Schema(description = "Sort criterion (created, updated, comments, reactions)", example = "updated", defaultValue = "updated")
    private String sort = "updated";

    @Schema(description = "Sort order (asc, desc)", example = "desc", defaultValue = "desc")
    private String order = "desc";

    @Min(value = 1, message = "Page number must be at least 1")
    @Schema(description = "Page number (1-based)", example = "1", defaultValue = "1")
    private Integer page = 1;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    @Schema(description = "Items per page (max 100)", example = "30", defaultValue = "30")
    private Integer perPage = 30;

    public PublicDiscoveryRequestDto() {
    }

    public PublicDiscoveryRequestDto(String query, String language, String repository, List<String> labels,
                                     String state, String sort, String order, Integer page, Integer perPage) {
        this.query = query;
        this.language = language;
        this.repository = repository;
        this.labels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
        this.state = state != null ? state : "open";
        this.sort = sort != null ? sort : "updated";
        this.order = order != null ? order : "desc";
        this.page = page != null ? page : 1;
        this.perPage = perPage != null ? perPage : 30;
    }

    /**
     * Checks whether at least one meaningful filter is provided.
     * Prevents unbounded wildcard scans against GitHub's public API.
     */
    public boolean hasAtLeastOneFilter() {
        return (query != null && !query.isBlank()) ||
                (language != null && !language.isBlank()) ||
                (repository != null && !repository.isBlank()) ||
                (labels != null && !labels.isEmpty() && labels.stream().anyMatch(l -> l != null && !l.isBlank()));
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getRepository() {
        return repository;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(List<String> labels) {
        this.labels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public Integer getPage() {
        return page != null ? page : 1;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getPerPage() {
        return perPage != null ? perPage : 30;
    }

    public void setPerPage(Integer perPage) {
        this.perPage = perPage;
    }
}
