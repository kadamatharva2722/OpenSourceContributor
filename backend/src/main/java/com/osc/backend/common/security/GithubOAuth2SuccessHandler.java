package com.osc.backend.common.security;

import com.osc.backend.user.dto.UserDto;
import com.osc.backend.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Dedicated authentication success handler for GitHub OAuth 2.0 flow.
 * Extracts GitHub identity, calls UserService to synchronize the local user profile,
 * builds the AppUserPrincipal, binds it to the SecurityContext, and redirects to the frontend.
 */
@Component
public class GithubOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GithubOAuth2SuccessHandler.class);

    private final UserService userService;
    private final GithubOAuth2FailureHandler failureHandler;
    private final String frontendUrl;
    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    private final SessionRegistry sessionRegistry;

    public GithubOAuth2SuccessHandler(
            UserService userService,
            GithubOAuth2FailureHandler failureHandler,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl
    ) {
        this(userService, failureHandler, frontendUrl, null);
    }

    @Autowired
    public GithubOAuth2SuccessHandler(
            UserService userService,
            GithubOAuth2FailureHandler failureHandler,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl,
            @Lazy @Autowired(required = false) SessionRegistry sessionRegistry
    ) {
        this.userService = userService;
        this.failureHandler = failureHandler;
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException {
        try {
            if (!(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_user_info"),
                        "OAuth2 principal is not an instance of OAuth2User"
                );
            }

            Map<String, Object> attributes = oauth2User.getAttributes();

            // 1 & 2. Extract and validate numeric GitHub ID (stable identity)
            Long githubId = extractGithubId(attributes.get("id"));

            // 3. Extract and validate GitHub username (login)
            String username = extractStringAttribute(attributes.get("login"));
            if (username == null || username.trim().isEmpty()) {
                throw new OAuth2AuthenticationException(
                        new OAuth2Error("invalid_user_info"),
                        "GitHub username ('login') is missing from user attributes"
                );
            }

            // 4, 5, 6. Extract optional profile fields
            String displayName = extractStringAttribute(attributes.get("name"));
            String avatarUrl = extractStringAttribute(attributes.get("avatar_url"));
            String bio = extractStringAttribute(attributes.get("bio"));

            log.info("Processing GitHub OAuth login for githubId={}, username={}", githubId, username);

            // 8 & 9. Local user persistence / synchronization
            UserDto localUser = userService.getOrCreateUser(githubId, username, displayName, avatarUrl, bio);

            // 10. Construct domain AppUserPrincipal with current authentication timestamp
            AppUserPrincipal appUserPrincipal = new AppUserPrincipal(
                    localUser.getId(),
                    localUser.getGithubId(),
                    localUser.getUsername(),
                    attributes,
                    authentication.getAuthorities(),
                    Instant.now()
            );

            // 11. Bind authenticated principal into SecurityContext
            String registrationId = authentication instanceof OAuth2AuthenticationToken oauthToken
                    ? oauthToken.getAuthorizedClientRegistrationId()
                    : "github";

            OAuth2AuthenticationToken updatedAuthentication = new OAuth2AuthenticationToken(
                    appUserPrincipal,
                    authentication.getAuthorities(),
                    registrationId
            );

            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(updatedAuthentication);
            SecurityContextHolder.setContext(securityContext);

            // 12. Persist SecurityContext for HTTP session-based authentication
            securityContextRepository.saveContext(securityContext, request, response);

            // 13. Register session in SessionRegistry for cluster/session management
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null && sessionRegistry != null) {
                sessionRegistry.registerNewSession(session.getId(), appUserPrincipal);
            }

            // 14. Redirect to the configured frontend URL (no tokens exposed in URL)
            response.sendRedirect(frontendUrl);

        } catch (Exception ex) {
            log.error("Failed to complete GitHub OAuth success processing: {}", ex.getMessage(), ex);
            OAuth2AuthenticationException oauthEx = ex instanceof OAuth2AuthenticationException oAuth2Exception
                    ? oAuth2Exception
                    : new OAuth2AuthenticationException(new OAuth2Error("user_persistence_failure"), ex.getMessage(), ex);
            failureHandler.onAuthenticationFailure(request, response, oauthEx);
        }
    }

    private Long extractGithubId(Object idObj) {
        if (idObj == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "GitHub numeric account ID ('id') is missing"
            );
        }
        if (idObj instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(idObj.toString());
        } catch (NumberFormatException ex) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_user_info"),
                    "GitHub account ID ('id') is not numeric: " + idObj
            );
        }
    }

    private String extractStringAttribute(Object attrObj) {
        if (attrObj == null) {
            return null;
        }
        String value = attrObj.toString().trim();
        return value.isEmpty() ? null : value;
    }
}
