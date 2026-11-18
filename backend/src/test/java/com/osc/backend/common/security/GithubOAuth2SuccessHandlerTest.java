package com.osc.backend.common.security;

import com.osc.backend.user.dto.UserDto;
import com.osc.backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GithubOAuth2SuccessHandlerTest {

    @Mock
    private UserService userService;

    @Mock
    private GithubOAuth2FailureHandler failureHandler;

    private GithubOAuth2SuccessHandler successHandler;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        successHandler = new GithubOAuth2SuccessHandler(userService, failureHandler, "http://localhost:5173");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("onAuthenticationSuccess synchronizes user, builds AppUserPrincipal and redirects to frontend")
    void onAuthenticationSuccess_ValidUser_Succeeds() throws IOException {
        Map<String, Object> attributes = Map.of(
                "id", 12345678,
                "login", "octocat",
                "name", "The Octocat",
                "avatar_url", "https://github.com/images/error/octocat_happy.gif",
                "bio", "GitHub mascot"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(
                oauth2User,
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                "github"
        );

        UserDto expectedUserDto = new UserDto(
                1L, 12345678L, "octocat", "The Octocat",
                "https://github.com/images/error/octocat_happy.gif", "GitHub mascot",
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(userService.getOrCreateUser(12345678L, "octocat", "The Octocat",
                "https://github.com/images/error/octocat_happy.gif", "GitHub mascot"))
                .thenReturn(expectedUserDto);

        successHandler.onAuthenticationSuccess(request, response, authToken);

        verify(userService).getOrCreateUser(12345678L, "octocat", "The Octocat",
                "https://github.com/images/error/octocat_happy.gif", "GitHub mascot");
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173");

        SecurityContext securityContext = SecurityContextHolder.getContext();
        assertThat(securityContext.getAuthentication()).isNotNull();
        assertThat(securityContext.getAuthentication().getPrincipal()).isInstanceOf(AppUserPrincipal.class);

        AppUserPrincipal principal = (AppUserPrincipal) securityContext.getAuthentication().getPrincipal();
        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getGithubId()).isEqualTo(12345678L);
        assertThat(principal.getUsername()).isEqualTo("octocat");
    }

    @Test
    @DisplayName("onAuthenticationSuccess delegates to failure handler when GitHub login username is missing")
    void onAuthenticationSuccess_MissingLogin_DelegatesToFailureHandler() throws IOException {
        Map<String, Object> attributes = Map.of(
                "id", 12345678
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(
                oauth2User,
                Collections.emptyList(),
                "github"
        );

        successHandler.onAuthenticationSuccess(request, response, authToken);

        verify(failureHandler).onAuthenticationFailure(eq(request), eq(response), any(OAuth2AuthenticationException.class));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("onAuthenticationSuccess delegates to failure handler when GitHub numeric ID is missing")
    void onAuthenticationSuccess_MissingId_DelegatesToFailureHandler() throws IOException {
        Map<String, Object> attributes = Map.of(
                "login", "octocat"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "login"
        );
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(
                oauth2User,
                Collections.emptyList(),
                "github"
        );

        successHandler.onAuthenticationSuccess(request, response, authToken);

        verify(failureHandler).onAuthenticationFailure(eq(request), eq(response), any(OAuth2AuthenticationException.class));
        verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("onAuthenticationSuccess delegates to failure handler when userService throws exception")
    void onAuthenticationSuccess_UserServiceError_DelegatesToFailureHandler() throws IOException {
        Map<String, Object> attributes = Map.of(
                "id", 9999L,
                "login", "faulty_user"
        );
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "id"
        );
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(
                oauth2User,
                Collections.emptyList(),
                "github"
        );

        when(userService.getOrCreateUser(any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Database connectivity failure"));

        successHandler.onAuthenticationSuccess(request, response, authToken);

        ArgumentCaptor<OAuth2AuthenticationException> exceptionCaptor =
                ArgumentCaptor.forClass(OAuth2AuthenticationException.class);
        verify(failureHandler).onAuthenticationFailure(eq(request), eq(response), exceptionCaptor.capture());
        assertThat(exceptionCaptor.getValue().getError().getErrorCode()).isEqualTo("user_persistence_failure");
    }
}
