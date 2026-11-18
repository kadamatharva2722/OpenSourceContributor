package com.osc.backend.admin.dto;

/**
 * Data transfer object representing the current multi-step authentication status of the administrator console.
 */
public class AdminStatusDto {

    private boolean authenticated;
    private boolean admin;
    private String username;
    private Long githubId;
    private String authState; // UNAUTHENTICATED, GITHUB_AUTHENTICATED, PASSWORD_REQUIRED, ADMIN_VERIFIED
    private boolean passwordRequired;

    public AdminStatusDto() {
    }

    public AdminStatusDto(boolean authenticated, boolean admin, String username, Long githubId) {
        this(authenticated, admin, username, githubId,
                admin ? "ADMIN_VERIFIED" : (authenticated ? "GITHUB_AUTHENTICATED" : "UNAUTHENTICATED"),
                false);
    }

    public AdminStatusDto(boolean authenticated, boolean admin, String username, Long githubId, String authState, boolean passwordRequired) {
        this.authenticated = authenticated;
        this.admin = admin;
        this.username = username;
        this.githubId = githubId;
        this.authState = authState;
        this.passwordRequired = passwordRequired;
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

    public String getAuthState() {
        return authState;
    }

    public void setAuthState(String authState) {
        this.authState = authState;
    }

    public boolean isPasswordRequired() {
        return passwordRequired;
    }

    public void setPasswordRequired(boolean passwordRequired) {
        this.passwordRequired = passwordRequired;
    }
}
