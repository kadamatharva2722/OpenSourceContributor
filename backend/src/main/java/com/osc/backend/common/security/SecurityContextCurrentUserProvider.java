package com.osc.backend.common.security;

import com.osc.backend.common.exception.UnauthenticatedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

/**
 * Spring Security SecurityContextHolder implementation of CurrentUserProvider.
 * Extracts the AppUserPrincipal from the current Authentication instance.
 */
@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {
        return getCurrentPrincipal().getUserId();
    }

    @Override
    public AppUserPrincipal getCurrentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new UnauthenticatedException("Authentication is required to access this resource");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserPrincipal appUserPrincipal) {
            return appUserPrincipal;
        }

        if (principal instanceof OAuth2User oauth2User) {
            Object userIdAttr = oauth2User.getAttribute("localUserId");
            Object githubIdAttr = oauth2User.getAttribute("id");
            Object loginAttr = oauth2User.getAttribute("login");
            if (userIdAttr instanceof Long lUserId && githubIdAttr instanceof Number gId && loginAttr instanceof String login) {
                return new AppUserPrincipal(lUserId, gId.longValue(), login, oauth2User.getAttributes(), oauth2User.getAuthorities());
            }
        }

        throw new UnauthenticatedException("User is not authenticated with a valid application principal");
    }
}
