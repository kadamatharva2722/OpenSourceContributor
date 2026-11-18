package com.osc.backend.request.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RepositoryRequestCreateDto {

    @NotBlank(message = "Repository URL is required")
    private String repositoryUrl;

    @NotBlank(message = "Repository name is required")
    @Size(max = 255, message = "Repository name must not exceed 255 characters")
    private String repositoryName;

    @Size(max = 2000, message = "Reason must not exceed 2000 characters")
    private String reason;

    @Size(max = 50, message = "Username must not exceed 50 characters")
    private String requestedByUsername;

    public RepositoryRequestCreateDto() {
    }

    public RepositoryRequestCreateDto(String repositoryUrl, String repositoryName, String reason, String requestedByUsername) {
        this.repositoryUrl = repositoryUrl;
        this.repositoryName = repositoryName;
        this.reason = reason;
        this.requestedByUsername = requestedByUsername;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestedByUsername() {
        return requestedByUsername;
    }

    public void setRequestedByUsername(String requestedByUsername) {
        this.requestedByUsername = requestedByUsername;
    }
}
