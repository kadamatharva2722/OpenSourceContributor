package com.osc.backend.common.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GithubOAuth2FailureHandlerTest {

    private GithubOAuth2FailureHandler failureHandler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        failureHandler = new GithubOAuth2FailureHandler("http://localhost:5173");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("onAuthenticationFailure redirects with access_denied when user denies authorization")
    void onAuthenticationFailure_AccessDenied_RedirectsWithAccessDeniedCode() throws IOException {
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("access_denied"),
                "User denied authorization"
        );

        failureHandler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?error=access_denied");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("onAuthenticationFailure redirects with invalid_state when state mismatch occurs")
    void onAuthenticationFailure_InvalidState_RedirectsWithInvalidStateCode() throws IOException {
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("invalid_state"),
                "State parameter did not match"
        );

        failureHandler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?error=invalid_state");
    }

    @Test
    @DisplayName("onAuthenticationFailure redirects with user_persistence_failure on database synchronization error")
    void onAuthenticationFailure_UserPersistenceFailure_RedirectsWithUserPersistenceCode() throws IOException {
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("user_persistence_failure"),
                "Failed to synchronize user"
        );

        failureHandler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?error=user_persistence_failure");
    }

    @Test
    @DisplayName("onAuthenticationFailure redirects with oauth_failed on generic failure")
    void onAuthenticationFailure_GenericFailure_RedirectsWithOauthFailedCode() throws IOException {
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("server_error"),
                "Unexpected failure"
        );

        failureHandler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/login?error=oauth_failed");
    }
}
