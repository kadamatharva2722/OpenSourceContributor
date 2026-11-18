package com.osc.backend.discovery.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

/**
 * Request payload for AI-powered issue discovery search.
 */
@Schema(description = "Discovery search request containing natural language prompt and pagination options")
public class DiscoverySearchRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Search prompt must not be blank")
    @Size(min = 3, max = 500, message = "Search prompt must be between 3 and 500 characters")
    @Schema(description = "Natural language search prompt", example = "Find beginner-friendly Java issues in active repositories with good first issue labels")
    private String prompt;

    @Min(value = 1, message = "Page number must be at least 1")
    @Schema(description = "Page number (1-based)", example = "1", defaultValue = "1")
    private Integer page = 1;

    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = 100, message = "Page size cannot exceed 100")
    @Schema(description = "Items per page (max 100)", example = "30", defaultValue = "30")
    private Integer perPage = 30;

    public DiscoverySearchRequestDto() {
    }

    public DiscoverySearchRequestDto(String prompt) {
        this.prompt = prompt;
    }

    public DiscoverySearchRequestDto(String prompt, Integer page, Integer perPage) {
        this.prompt = prompt;
        this.page = page != null ? page : 1;
        this.perPage = perPage != null ? perPage : 30;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
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
