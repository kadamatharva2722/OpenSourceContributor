package com.osc.backend.common.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import java.util.Collections;
import java.util.Iterator;

/**
 * Configuration for OAuth 2.0 client infrastructure.
 * Externalizes GitHub credentials and gracefully initializes in local development
 * without valid OAuth credentials when unconfigured.
 */
@Configuration
public class OAuth2ClientConfig {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientConfig.class);

    @Bean
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.oauth2.github.client-id:${spring.security.oauth2.client.registration.github.client-id:${GITHUB_CLIENT_ID:}}}") String clientId,
            @Value("${app.oauth2.github.client-secret:${spring.security.oauth2.client.registration.github.client-secret:${GITHUB_CLIENT_SECRET:}}}") String clientSecret,
            @Value("${app.oauth2.github.scope:${spring.security.oauth2.client.registration.github.scope:${GITHUB_OAUTH_SCOPE:read:user}}}") String scope,
            @Value("${app.oauth2.github.redirect-uri:${spring.security.oauth2.client.registration.github.redirect-uri:${GITHUB_REDIRECT_URI:{baseUrl}/login/oauth2/code/{registrationId}}}}") String redirectUri
    ) {
        if (clientId == null || clientId.trim().isEmpty() || clientSecret == null || clientSecret.trim().isEmpty()) {
            log.info("GitHub OAuth2 client credentials not configured. Initializing empty ClientRegistrationRepository for development/testing.");
            return new EmptyClientRegistrationRepository();
        }

        log.info("Configuring GitHub OAuth 2.0 ClientRegistration with scope '{}'", scope);

        String[] scopes = scope.contains(",") ? scope.split(",") : new String[]{scope.trim()};

        ClientRegistration registration = ClientRegistration.withRegistrationId("github")
                .clientId(clientId.trim())
                .clientSecret(clientSecret.trim())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope(scopes)
                .authorizationUri("https://github.com/login/oauth/authorize")
                .tokenUri("https://github.com/login/oauth/access_token")
                .userInfoUri("https://api.github.com/user")
                .userNameAttributeName("id")
                .clientName("GitHub")
                .build();

        return new InMemoryClientRegistrationRepository(registration);
    }

    @Bean
    @ConditionalOnMissingBean(OAuth2AuthorizedClientRepository.class)
    public OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new DiscardingOAuth2AuthorizedClientRepository();
    }

    /**
     * Fallback repository implementation representing an unconfigured or disabled OAuth2 client environment.
     * Prevents ApplicationContext startup failures when OAuth credentials are not provided.
     */
    public static class EmptyClientRegistrationRepository implements ClientRegistrationRepository, Iterable<ClientRegistration> {

        @Override
        public ClientRegistration findByRegistrationId(String registrationId) {
            return null;
        }

        @Override
        public Iterator<ClientRegistration> iterator() {
            return Collections.emptyIterator();
        }
    }
}
