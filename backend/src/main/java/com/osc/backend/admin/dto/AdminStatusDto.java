package com.osc.backend.admin.dto;

public class AdminStatusDto {

    private boolean authenticated;
    private boolean admin;
    private String username;
    private Long githubId;

    public AdminStatusDto() {
    }

    public AdminStatusDto(boolean authenticated, boolean admin, String username, Long githubId) {
        this.authenticated = authenticated;
        this.admin = admin;
        this.username = username;
        this.githubId = githubId;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public boolean isAdmin() {
        return admin;
    }

    public void setAdmin(boolean admin) {
        this.admin = admin;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getGithubId() {
        return githubId;
    }

    public void setGithubId(Long githubId) {
        this.githubId = githubId;
    }
}
