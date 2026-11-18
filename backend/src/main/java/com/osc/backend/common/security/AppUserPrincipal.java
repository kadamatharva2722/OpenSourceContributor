package com.osc.backend.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Domain principal representing the authenticated application user.
 * Encapsulates the local database identifier and permanent GitHub numeric ID.
 * Implements {@link OAuth2User} for seamless Spring Security OAuth2 integration.
 */
public class AppUserPrincipal implements OAuth2User, Serializable {

    private static final long serialVersionUID = 2L;

    private final Long userId;
    private final Long githubId;
    private final String username;
    private final Map<String, Object> attributes;
    private final Collection<? extends GrantedAuthority> authorities;
    private final Instant authenticatedAt;

    public AppUserPrincipal(Long userId, Long githubId, String username) {
        this(userId, githubId, username, Collections.emptyMap(), List.of(new SimpleGrantedAuthority("ROLE_USER")), Instant.now());
    }

    public AppUserPrincipal(Long userId, Long githubId, String username,
                            Map<String, Object> attributes,
                            Collection<? extends GrantedAuthority> authorities) {
        this(userId, githubId, username, attributes, authorities, Instant.now());
    }

    public AppUserPrincipal(Long userId, Long githubId, String username,
                            Map<String, Object> attributes,
                            Collection<? extends GrantedAuthority> authorities,
                            Instant authenticatedAt) {
        this.userId = userId;
        this.githubId = githubId;
        this.username = username;
        this.attributes = attributes != null ? Collections.unmodifiableMap(new HashMap<>(attributes)) : Collections.emptyMap();
        this.authorities = authorities != null ? authorities : List.of(new SimpleGrantedAuthority("ROLE_USER"));
        this.authenticatedAt = authenticatedAt != null ? authenticatedAt : Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public Long getGithubId() {
        return githubId;
    }

    public String getUsername() {
        return username;
    }

    public Instant getAuthenticatedAt() {
        return authenticatedAt;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getName() {
        return username != null ? username : (githubId != null ? String.valueOf(githubId) : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AppUserPrincipal that = (AppUserPrincipal) o;
        return java.util.Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(userId);
    }

    @Override
    public String toString() {
        return "AppUserPrincipal{" +
                "userId=" + userId +
                ", githubId=" + githubId +
                ", username='" + username + '\'' +
                '}';
    }
}
