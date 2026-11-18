package com.osc.backend.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Dedicated authentication failure handler for GitHub OAuth 2.0 flow.
 * Safely maps internal/provider errors to generic error codes and redirects to the frontend login route.
 */
@Component
public class GithubOAuth2FailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(GithubOAuth2FailureHandler.class);

    private final String frontendUrl;

    public GithubOAuth2FailureHandler(
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl
    ) {
        this.frontendUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException {
        SecurityContextHolder.clearContext();

        String errorCode = resolveErrorCode(exception);
        log.warn("GitHub OAuth authentication failed: error_code={}, exception={}", errorCode, exception.getMessage());

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                .queryParam("error", errorCode)
                .build()
                .toUriString();

        response.sendRedirect(targetUrl);
    }

    private String resolveErrorCode(AuthenticationException exception) {
        if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
            OAuth2Error error = oAuth2Exception.getError();
            if (error != null && error.getErrorCode() != null) {
                String code = error.getErrorCode().toLowerCase();
                if (code.contains("access_denied")) {
                    return "access_denied";
                }
                if (code.contains("invalid_state")) {
                    return "invalid_state";
                }
                if (code.contains("invalid_code") || code.contains("authorization_request_not_found")) {
                    return "invalid_request";
                }
                if (code.contains("invalid_token") || code.contains("token_response_error")) {
                    return "oauth_exchange_failed";
                }
                if (code.contains("invalid_user_info")) {
                    return "invalid_user_info";
                }
                if (code.contains("user_persistence_failure")) {
                    return "user_persistence_failure";
                }
            }
        }

        String msg = exception.getMessage() != null ? exception.getMessage().toLowerCase() : "";
        if (msg.contains("access_denied") || msg.contains("user denied") || msg.contains("denied")) {
            return "access_denied";
        }
        if (msg.contains("state")) {
            return "invalid_state";
        }
        if (msg.contains("persistence") || msg.contains("database")) {
            return "user_persistence_failure";
        }

        return "oauth_failed";
    }
}
