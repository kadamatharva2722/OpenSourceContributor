package com.osc.backend.userpreference.dto;

import com.osc.backend.userpreference.entity.UserPreference;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object representing user discovery preferences.
 */
public class UserPreferenceResponseDto {

    private Long id;
    private Long userId;
    private List<String> preferredLanguages = new ArrayList<>();
    private List<String> preferredDomains = new ArrayList<>();
    private String difficulty;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public UserPreferenceResponseDto() {
    }

    public UserPreferenceResponseDto(Long id, Long userId, List<String> preferredLanguages, List<String> preferredDomains,
                                     String difficulty, OffsetDateTime createdAt, OffsetDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.preferredLanguages = preferredLanguages != null ? preferredLanguages : new ArrayList<>();
        this.preferredDomains = preferredDomains != null ? preferredDomains : new ArrayList<>();
        this.difficulty = difficulty;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserPreferenceResponseDto fromEntity(UserPreference preference) {
        if (preference == null) {
            return null;
        }
        return new UserPreferenceResponseDto(
                preference.getId(),
                preference.getUser() != null ? preference.getUser().getId() : null,
                preference.getPreferredLanguages(),
                preference.getPreferredDomains(),
                preference.getDifficulty(),
                preference.getCreatedAt(),
                preference.getUpdatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<String> getPreferredLanguages() {
        return preferredLanguages;
    }

    public void setPreferredLanguages(List<String> preferredLanguages) {
        this.preferredLanguages = preferredLanguages != null ? preferredLanguages : new ArrayList<>();
    }

    public List<String> getPreferredDomains() {
        return preferredDomains;
    }

    public void setPreferredDomains(List<String> preferredDomains) {
        this.preferredDomains = preferredDomains != null ? preferredDomains : new ArrayList<>();
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
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
