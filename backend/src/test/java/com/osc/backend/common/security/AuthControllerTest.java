package com.osc.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.user.dto.UserDto;
import com.osc.backend.user.service.UserService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, SecurityContextCurrentUserProvider.class, OAuth2ClientConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private CurrentUserProvider currentUserProvider;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication createAuthentication(Long userId, Long githubId, String username) {
        AppUserPrincipal principal = new AppUserPrincipal(userId, githubId, username);
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }

    private void authenticateUser(Long userId, Long githubId, String username) {
        Authentication auth = createAuthentication(userId, githubId, username);
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("GET /api/v1/auth/csrf returns token when present on request")
    void getCsrfToken_Present_ReturnsToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/status returns unauthenticated when no session")
    void getAuthStatus_Unauthenticated_ReturnsFalse() throws Exception {
        mockMvc.perform(get("/api/v1/auth/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(false))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.username").doesNotExist());
    }

    @Test
    @DisplayName("GET /api/v1/auth/status returns authenticated with user details when logged in")
    void getAuthStatus_Authenticated_ReturnsTrue() throws Exception {
        Authentication auth = createAuthentication(1L, 1001L, "octocat");
        when(userService.existsById(1L)).thenReturn(true);

        mockMvc.perform(get("/api/v1/auth/status").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("octocat"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/me returns 401 when unauthenticated")
    void getCurrentUser_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/auth/me returns UserDto when authenticated")
    void getCurrentUser_Authenticated_ReturnsUser() throws Exception {
        Authentication auth = createAuthentication(1L, 1001L, "octocat");
        when(currentUserProvider.getCurrentUserId()).thenReturn(1L);

        UserDto userDto = new UserDto(
                1L, 1001L, "octocat", "The Octocat",
                "https://avatars.githubusercontent.com/u/1001", "Bio",
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(userService.findById(1L)).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/auth/me").with(authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.githubId").value(1001))
                .andExpect(jsonPath("$.username").value("octocat"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout invalidates session and clears context")
    void logout_InvalidatesSession() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Cookie csrfCookie = new Cookie("XSRF-TOKEN", "test-csrf-token-123");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .session(session)
                        .cookie(csrfCookie)
                        .header("X-XSRF-TOKEN", "test-csrf-token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        assertThat(session.isInvalid()).isTrue();
    }
}
