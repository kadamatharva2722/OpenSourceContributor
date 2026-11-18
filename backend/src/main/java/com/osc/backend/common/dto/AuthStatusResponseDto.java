package com.osc.backend.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data transfer object representing the current session authentication status.
 */
@Schema(description = "Session authentication status details")
public class AuthStatusResponseDto {

    @Schema(description = "Whether the current request has an authenticated session", example = "true")
    private boolean authenticated;

    @Schema(description = "Internal database user ID of the authenticated user (null if unauthenticated)", example = "1")
    private Long userId;

    @Schema(description = "GitHub username of the authenticated user (null if unauthenticated)", example = "octocat")
    private String username;

    public AuthStatusResponseDto() {
    }

    public AuthStatusResponseDto(boolean authenticated, Long userId, String username) {
        this.authenticated = authenticated;
        this.userId = userId;
        this.username = username;
    }

    public static AuthStatusResponseDto unauthenticated() {
        return new AuthStatusResponseDto(false, null, null);
    }

    public static AuthStatusResponseDto authenticated(Long userId, String username) {
        return new AuthStatusResponseDto(true, userId, username);
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "AuthStatusResponseDto{" +
                "authenticated=" + authenticated +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                '}';
    }
}
