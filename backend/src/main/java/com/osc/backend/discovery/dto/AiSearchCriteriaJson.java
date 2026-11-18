package com.osc.backend.discovery.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Raw deserialization target for the structured JSON response produced by the LLM.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiSearchCriteriaJson implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("keywords")
    private String keywords;

    @JsonProperty("language")
    private String language;

    @JsonProperty("repository")
    private String repository;

    @JsonProperty("labels")
    private List<String> labels = new ArrayList<>();

    @JsonProperty("state")
    private String state;

    @JsonProperty("sort")
    private String sort;

    @JsonProperty("order")
    private String order;

    @JsonProperty("difficulty")
    private String difficulty;

    public AiSearchCriteriaJson() {
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
        this.labels = labels != null ? labels : new ArrayList<>();
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

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }
}
