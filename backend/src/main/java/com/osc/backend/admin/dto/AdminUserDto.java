package com.osc.backend.admin.dto;

import com.osc.backend.user.entity.User;

import java.time.OffsetDateTime;

public class AdminUserDto {

    private Long id;
    private Long githubId;
    private String username;
    private String displayName;
    private String avatarUrl;
    private String bio;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public AdminUserDto() {
    }

    public AdminUserDto(Long id, Long githubId, String username, String displayName, String avatarUrl, String bio, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.githubId = githubId;
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static AdminUserDto fromEntity(User user) {
        return new AdminUserDto(
                user.getId(),
                user.getGithubId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getBio(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getGithubId() {
        return githubId;
    }

    public void setGithubId(Long githubId) {
        this.githubId = githubId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
