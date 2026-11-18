package com.osc.backend.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/**
 * A hardened OAuth2AuthorizedClientRepository that immediately discards OAuth2 authorized clients.
 *
 * OpenSourceContributor only utilizes GitHub OAuth for user identity authentication and session creation.
 * The application does not perform authenticated GitHub API requests on behalf of users (all public issue
 * discovery uses unauthenticated or application-level REST calls).
 *
 * Storing the user's raw GitHub OAuth access token in the HttpSession or database represents an unnecessary
 * security surface. This repository ensures that OAuth2 tokens are NEVER persisted in memory, sessions,
 * or persistent storage after the authentication callback completes.
 */
public class DiscardingOAuth2AuthorizedClientRepository implements OAuth2AuthorizedClientRepository {

    private static final Logger log = LoggerFactory.getLogger(DiscardingOAuth2AuthorizedClientRepository.class);

    @Override
    @SuppressWarnings("unchecked")
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(
            String clientRegistrationId,
            Authentication principal,
            HttpServletRequest request
    ) {
        // Return null: no authorized client / access token is stored or retrieved
        return null;
    }

    @Override
    public void saveAuthorizedClient(
            OAuth2AuthorizedClient authorizedClient,
            Authentication principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // Deliberately discard the authorized client and its raw access token.
        // We log at debug level confirming token disposal without logging credentials.
        if (log.isDebugEnabled()) {
            log.debug("Discarding OAuth2AuthorizedClient for principal '{}' and client registration '{}' per zero-token retention policy.",
                    principal != null ? principal.getName() : "anonymous",
                    authorizedClient != null && authorizedClient.getClientRegistration() != null
                            ? authorizedClient.getClientRegistration().getRegistrationId()
                            : "unknown");
        }
    }

    @Override
    public void removeAuthorizedClient(
            String clientRegistrationId,
            Authentication principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        // No-op: nothing was stored to remove
    }
}
