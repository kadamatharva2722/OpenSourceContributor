package com.osc.backend.discovery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Validated and structured search criteria interpreted by AI from the user prompt.
 */
@Schema(description = "Structured search criteria extracted and validated from user prompt")
public class ParsedFilterDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "Keywords to search in issue title/body", example = "memory leak")
    private String keywords;

    @Schema(description = "Programming language filter", example = "Java")
    private String language;

    @Schema(description = "Target repository in owner/repo format", example = "facebook/react")
    private String repository;

    @Schema(description = "Labels to match", example = "[\"good first issue\", \"help wanted\"]")
    private List<String> labels = new ArrayList<>();

    @Schema(description = "Issue state (default: open)", example = "open")
    private String state = "open";

    @Schema(description = "Sort criterion", example = "updated")
    private String sort;

    @Schema(description = "Sort direction (asc/desc)", example = "desc")
    private String order;

    @Schema(description = "User's stated difficulty preference (e.g. beginner, intermediate, advanced)", example = "beginner")
    private String difficultyPreference;

    @Schema(description = "Page number", example = "1")
    private int page = 1;

    @Schema(description = "Page size (max 100)", example = "30")
    private int perPage = 30;

    public ParsedFilterDto() {
    }

    public ParsedFilterDto(String keywords, String language, String repository, List<String> labels,
                           String state, String sort, String order, String difficultyPreference,
                           int page, int perPage) {
        this.keywords = keywords;
        this.language = language;
        this.repository = repository;
        this.labels = labels != null ? new ArrayList<>(labels) : new ArrayList<>();
        this.state = state != null ? state : "open";
        this.sort = sort;
        this.order = order;
        this.difficultyPreference = difficultyPreference;
        this.page = page;
        this.perPage = perPage;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
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

    public String getDifficultyPreference() {
        return difficultyPreference;
    }

    public void setDifficultyPreference(String difficultyPreference) {
        this.difficultyPreference = difficultyPreference;
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
}
