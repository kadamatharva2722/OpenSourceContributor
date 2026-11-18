package com.osc.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating a user's profile details.
 */
public class UpdateUserProfileRequestDto {

    @NotBlank(message = "Username cannot be blank")
    @Size(max = 39, message = "Username must not exceed 39 characters")
    private String username;

    @Size(max = 255, message = "Display name must not exceed 255 characters")
    private String displayName;

    private String avatarUrl;

    private String bio;

    public UpdateUserProfileRequestDto() {
    }

    public UpdateUserProfileRequestDto(String username, String displayName, String avatarUrl, String bio) {
        this.username = username;
        this.displayName = displayName;
        this.avatarUrl = avatarUrl;
        this.bio = bio;
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
}
