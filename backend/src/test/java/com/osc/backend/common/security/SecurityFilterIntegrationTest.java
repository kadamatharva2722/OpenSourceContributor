package com.osc.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.account.controller.AccountController;
import com.osc.backend.account.service.AccountService;
import com.osc.backend.contribution.controller.ContributionController;
import com.osc.backend.contribution.service.ContributionService;
import com.osc.backend.discovery.controller.DiscoveryController;
import com.osc.backend.discovery.dto.DiscoverySearchRequestDto;
import com.osc.backend.discovery.dto.DiscoverySearchResponseDto;
import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.discovery.service.DiscoveryService;
import com.osc.backend.savedissue.controller.SavedIssueController;
import com.osc.backend.savedissue.dto.SaveIssueRequestDto;
import com.osc.backend.savedissue.dto.SavedIssueResponseDto;
import com.osc.backend.savedissue.service.SavedIssueService;
import com.osc.backend.user.controller.UserController;
import com.osc.backend.user.dto.UserDto;
import com.osc.backend.user.service.UserService;
import com.osc.backend.userpreference.controller.UserPreferenceController;
import com.osc.backend.userpreference.dto.UpdateUserPreferenceRequestDto;
import com.osc.backend.userpreference.dto.UserPreferenceResponseDto;
import com.osc.backend.userpreference.service.UserPreferenceService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        UserController.class,
        SavedIssueController.class,
        UserPreferenceController.class,
        AuthController.class,
        DiscoveryController.class,
        ContributionController.class,
        AccountController.class
})
@Import({SecurityConfig.class, SecurityContextCurrentUserProvider.class, OAuth2ClientConfig.class})
class SecurityFilterIntegrationTest {

    private static final String CSRF_TOKEN_VALUE = "test-csrf-token-abc123xyz";
    private static final Cookie CSRF_COOKIE = new Cookie("XSRF-TOKEN", CSRF_TOKEN_VALUE);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private SavedIssueService savedIssueService;

    @MockBean
    private UserPreferenceService userPreferenceService;

    @MockBean
    private DiscoveryService discoveryService;

    @MockBean
    private ClientIpResolver clientIpResolver;

    @MockBean
    private ClientIpRateLimiter clientIpRateLimiter;

    @MockBean
    private ContributionService contributionService;

    @MockBean
    private AccountService accountService;

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

    // -------------------------------------------------------------------------
    // 1. Unauthenticated Requests (401 Verification)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Unauthenticated GET /api/v1/users/me should be rejected by Spring Security filter with 401")
    void unauthenticatedGetRequest_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me"));
    }

    @Test
    @DisplayName("Unauthenticated POST /api/v1/saved-issues with CSRF token should be rejected with 401")
    void unauthenticatedPostRequest_WithCsrf_Returns401() throws Exception {
        SaveIssueRequestDto request = new SaveIssueRequestDto(123L, "owner/repo", 1, "Issue title", "https://issue/1");

        mockMvc.perform(post("/api/v1/saved-issues")
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .cookie(CSRF_COOKIE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Unauthenticated POST /api/v1/discovery/search with CSRF token should be rejected with 401")
    void unauthenticatedPostDiscoverySearch_WithCsrf_Returns401() throws Exception {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find beginner Java issues", 1, 30);

        mockMvc.perform(post("/api/v1/discovery/search")
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .cookie(CSRF_COOKIE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/api/v1/discovery/search"));

        verify(discoveryService, never()).discoverIssues(any());
    }

    @Test
    @DisplayName("Unauthenticated GET /api/v1/saved-issues should be rejected with 401")
    void unauthenticatedGetSavedIssues_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/saved-issues"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Unauthenticated GET /api/v1/contributions should be rejected with 401")
    void unauthenticatedGetContributions_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/contributions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("Unauthenticated GET /api/v1/preferences should be rejected with 401")
    void unauthenticatedGetPreferences_Returns401() throws Exception {
        mockMvc.perform(get("/api/v1/preferences"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    // -------------------------------------------------------------------------
    // 2. CSRF Protection for State-Changing Requests (403 Rejection without token)
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Authenticated POST /api/v1/discovery/search without CSRF token must be rejected with 403 Forbidden")
    void authenticatedPostDiscoverySearch_WithoutCsrf_Returns403() throws Exception {
        Authentication auth = createAuthentication(1L, 1001L, "octocat");
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find beginner Java issues", 1, 30);

        mockMvc.perform(post("/api/v1/discovery/search")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(discoveryService, never()).discoverIssues(any());
    }

    @Test
    @DisplayName("Authenticated POST /api/v1/saved-issues without CSRF token must be rejected with 403 Forbidden")
    void authenticatedPostRequest_WithoutCsrf_Returns403() throws Exception {
        Authentication auth = createAuthentication(1L, 1001L, "octocat");
        SaveIssueRequestDto request = new SaveIssueRequestDto(123L, "owner/repo", 1, "Issue title", "https://issue/1");

        mockMvc.perform(post("/api/v1/saved-issues")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(savedIssueService, never()).saveIssue(any(), any());
    }

    @Test
    @DisplayName("Authenticated PUT /api/v1/preferences without CSRF token must be rejected with 403 Forbidden")
    void authenticatedPutRequest_WithoutCsrf_Returns403() throws Exception {
        Authentication auth = createAuthentication(1L, 1001L, "octocat");
        UpdateUserPreferenceRequestDto request = new UpdateUserPreferenceRequestDto(List.of("Java"), List.of("Web"), "BEGINNER");

        mockMvc.perform(put("/api/v1/preferences")
                        .with(authentication(auth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(userPreferenceService, never()).createOrUpdatePreferences(any(), any());
    }

    @Test
    @DisplayName("Authenticated DELETE /api/v1/saved-issues/{id} without CSRF token must be rejected with 403 Forbidden")
    void authenticatedDeleteRequest_WithoutCsrf_Returns403() throws Exception {
        Authentication auth = createAuthentication(1L, 1001L, "octocat");

        mockMvc.perform(delete("/api/v1/saved-issues/10")
                        .with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(savedIssueService, never()).removeSavedIssue(any(), any());
    }

    // -------------------------------------------------------------------------
    // 3. Valid CSRF Tokens Allow Authenticated Requests
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Authenticated POST /api/v1/discovery/search with valid CSRF token should succeed with 200 OK")
    void authenticatedPostDiscoverySearch_WithValidCsrf_Succeeds() throws Exception {
        Authentication auth = createAuthentication(1L, 1001L, "octocat");
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find beginner Java issues", 1, 30);

        ParsedFilterDto filter = new ParsedFilterDto(null, "Java", null, List.of("good first issue"), "open", null, null, "beginner", 1, 30);
        DiscoverySearchResponseDto responseDto = new DiscoverySearchResponseDto(
                request.getPrompt(), filter, Collections.emptyList(), 0L, 1, 30, false, false, 19, null, "AUTHENTICATED_AI"
        );

        when(discoveryService.discoverIssues(any(DiscoverySearchRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/discovery/search")
                        .with(authentication(auth))
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .cookie(CSRF_COOKIE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discoveryMode").value("AUTHENTICATED_AI"))
                .andExpect(jsonPath("$.remainingDailyQuota").value(19))
                .andExpect(jsonPath("$.prompt").value("Find beginner Java issues"));

        verify(discoveryService).discoverIssues(any(DiscoverySearchRequestDto.class));
    }

    @Test
    @DisplayName("Authenticated POST /api/v1/saved-issues with valid CSRF token should succeed with 201 Created")
    void authenticatedPostRequest_WithValidCsrf_Succeeds() throws Exception {
        Authentication auth = createAuthentication(1L, 1001L, "octocat");
        SaveIssueRequestDto request = new SaveIssueRequestDto(123L, "owner/repo", 1, "Issue title", "https://issue/1");
        SavedIssueResponseDto responseDto = new SavedIssueResponseDto(
                55L, 1L, 123L, "owner/repo", 1, "Issue title", "https://issue/1", OffsetDateTime.now()
        );

        when(savedIssueService.saveIssue(eq(1L), any(SaveIssueRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/saved-issues")
                        .with(authentication(auth))
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .cookie(CSRF_COOKIE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(55))
                .andExpect(jsonPath("$.repositoryFullName").value("owner/repo"));

        verify(savedIssueService).saveIssue(eq(1L), any(SaveIssueRequestDto.class));
    }

    @Test
    @DisplayName("Authenticated PUT /api/v1/preferences with valid CSRF token should succeed with 200 OK")
    void authenticatedPutRequest_WithValidCsrf_Succeeds() throws Exception {
        Authentication auth = createAuthentication(1L, 1001L, "octocat");
        UpdateUserPreferenceRequestDto request = new UpdateUserPreferenceRequestDto(List.of("Rust"), List.of("Systems"), "ADVANCED");
        UserPreferenceResponseDto responseDto = new UserPreferenceResponseDto(
                10L, 1L, List.of("Rust"), List.of("Systems"), "ADVANCED", OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(userPreferenceService.createOrUpdatePreferences(eq(1L), any(UpdateUserPreferenceRequestDto.class))).thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/preferences")
                        .with(authentication(auth))
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .cookie(CSRF_COOKIE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.difficulty").value("ADVANCED"));

        verify(userPreferenceService).createOrUpdatePreferences(eq(1L), any(UpdateUserPreferenceRequestDto.class));
    }

    @Test
    @DisplayName("Authenticated DELETE /api/v1/saved-issues/{id} with valid CSRF token should succeed with 204 No Content")
    void authenticatedDeleteRequest_WithValidCsrf_Succeeds() throws Exception {
        Authentication auth = createAuthentication(1L, 1001L, "octocat");
        doNothing().when(savedIssueService).removeSavedIssue(10L, 1L);

        mockMvc.perform(delete("/api/v1/saved-issues/10")
                        .with(authentication(auth))
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .cookie(CSRF_COOKIE))
                .andExpect(status().isNoContent());

        verify(savedIssueService).removeSavedIssue(10L, 1L);
    }

    // -------------------------------------------------------------------------
    // 4. Safe Public Endpoints & CSRF Token Exposure
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Unauthenticated GET /api/v1/discovery/public should be publicly accessible without auth or CSRF")
    void unauthenticatedGetDiscoveryPublic_Returns200() throws Exception {
        ParsedFilterDto filter = new ParsedFilterDto("spring", "Java", null, List.of("good first issue"), "open", "updated", "desc", null, 1, 30);
        DiscoverySearchResponseDto responseDto = new DiscoverySearchResponseDto(
                null, filter, Collections.emptyList(), 0L, 1, 30, false, false, null, null, "GUEST"
        );

        when(clientIpResolver.resolveClientIp(any())).thenReturn("127.0.0.1");
        when(clientIpRateLimiter.getRateLimitInfo(any())).thenReturn(new ClientIpRateLimiter.RateLimitInfo(60, 59, 1700000000L));
        when(discoveryService.browsePublicIssues(any(), eq("127.0.0.1"))).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/discovery/public")
                        .param("query", "spring")
                        .param("language", "Java"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-RateLimit-Limit", "60"))
                .andExpect(header().string("X-RateLimit-Remaining", "59"))
                .andExpect(jsonPath("$.discoveryMode").value("GUEST"))
                .andExpect(jsonPath("$.remainingDailyQuota").doesNotExist());

        verify(discoveryService).browsePublicIssues(any(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("GET /api/v1/auth/csrf should be publicly accessible, return active CSRF token details and set cookie")
    void getCsrfToken_ReturnsTokenAndSetsCookie() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.parameterName").value("_csrf"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    @DisplayName("DELETE /api/v1/account without CSRF token should be rejected with 403 Forbidden")
    void deleteAccount_WithoutCsrf_Returns403() throws Exception {
        authenticateUser(1L, 1001L, "alice");

        mockMvc.perform(delete("/api/v1/account")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(accountService, never()).deleteAccount(any(), any(), any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/account with invalid CSRF token should be rejected with 403 Forbidden")
    void deleteAccount_WithInvalidCsrf_Returns403() throws Exception {
        authenticateUser(1L, 1001L, "alice");

        mockMvc.perform(delete("/api/v1/account")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", "wrong-csrf-token")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));

        verify(accountService, never()).deleteAccount(any(), any(), any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/account unauthenticated should be rejected with 401 Unauthorized")
    void deleteAccount_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(delete("/api/v1/account")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(accountService, never()).deleteAccount(any(), any(), any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/account authenticated with valid CSRF should succeed with 200 OK")
    void deleteAccount_AuthenticatedWithCsrf_Returns200() throws Exception {
        authenticateUser(1L, 1001L, "alice");
        doNothing().when(accountService).deleteAccount(eq(1L), any(), any(), any());

        mockMvc.perform(delete("/api/v1/account")
                        .cookie(CSRF_COOKIE)
                        .header("X-XSRF-TOKEN", CSRF_TOKEN_VALUE)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account and associated data deleted successfully"));

        verify(accountService).deleteAccount(eq(1L), any(), any(), any());
    }
}
