package com.osc.backend.request.dto;

import com.osc.backend.request.entity.RepositoryRequest;

import java.time.OffsetDateTime;

public class RepositoryRequestResponseDto {

    private Long id;
    private String repositoryUrl;
    private String repositoryName;
    private String reason;
    private String requestedByUsername;
    private String status;
    private OffsetDateTime createdAt;

    public RepositoryRequestResponseDto() {
    }

    public RepositoryRequestResponseDto(Long id, String repositoryUrl, String repositoryName, String reason,
                                        String requestedByUsername, String status, OffsetDateTime createdAt) {
        this.id = id;
        this.repositoryUrl = repositoryUrl;
        this.repositoryName = repositoryName;
        this.reason = reason;
        this.requestedByUsername = requestedByUsername;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static RepositoryRequestResponseDto fromEntity(RepositoryRequest entity) {
        return new RepositoryRequestResponseDto(
                entity.getId(),
                entity.getRepositoryUrl(),
                entity.getRepositoryName(),
                entity.getReason(),
                entity.getRequestedByUsername(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
