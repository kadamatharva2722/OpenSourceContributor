package com.osc.backend.userpreference.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Request payload for creating or updating user discovery preferences.
 */
public class UpdateUserPreferenceRequestDto {

    private List<String> preferredLanguages = new ArrayList<>();
    private List<String> preferredDomains = new ArrayList<>();
    private String difficulty;

    public UpdateUserPreferenceRequestDto() {
    }

    public UpdateUserPreferenceRequestDto(List<String> preferredLanguages, List<String> preferredDomains, String difficulty) {
        this.preferredLanguages = preferredLanguages != null ? preferredLanguages : new ArrayList<>();
        this.preferredDomains = preferredDomains != null ? preferredDomains : new ArrayList<>();
        this.difficulty = difficulty;
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
}
