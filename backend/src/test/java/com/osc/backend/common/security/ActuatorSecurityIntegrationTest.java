package com.osc.backend.common.security;

import com.osc.backend.aiusage.repository.AiUsageRepository;
import com.osc.backend.contribution.repository.ContributionRepository;
import com.osc.backend.savedissue.repository.SavedIssueRepository;
import com.osc.backend.user.repository.UserRepository;
import com.osc.backend.userpreference.repository.UserPreferenceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserPreferenceRepository userPreferenceRepository;

    @MockBean
    private SavedIssueRepository savedIssueRepository;

    @MockBean
    private ContributionRepository contributionRepository;

    @MockBean
    private AiUsageRepository aiUsageRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Authentication createAuthentication() {
        AppUserPrincipal principal = new AppUserPrincipal(1L, 1001L, "octocat");
        return new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
    }

    private void authenticateUser() {
        Authentication auth = createAuthentication();
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("GET /actuator/health is publicly accessible without authentication")
    void healthEndpoint_IsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("GET /actuator/info is publicly accessible without authentication")
    void infoEndpoint_IsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /actuator/env is rejected with 401 for unauthenticated public requests")
    void envEndpoint_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/actuator/env"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("GET /actuator/env is denied with 403 Forbidden even when authenticated")
    void envEndpoint_Authenticated_Returns403() throws Exception {
        Authentication auth = createAuthentication();
        mockMvc.perform(get("/actuator/env").with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("GET /actuator/beans is rejected with 401 for unauthenticated requests")
    void beansEndpoint_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/actuator/beans"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /actuator/beans is denied with 403 Forbidden even when authenticated")
    void beansEndpoint_Authenticated_Returns403() throws Exception {
        Authentication auth = createAuthentication();
        mockMvc.perform(get("/actuator/beans").with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /actuator/mappings is rejected with 401 for unauthenticated requests")
    void mappingsEndpoint_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/actuator/mappings"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /actuator/mappings is denied with 403 Forbidden even when authenticated")
    void mappingsEndpoint_Authenticated_Returns403() throws Exception {
        Authentication auth = createAuthentication();
        mockMvc.perform(get("/actuator/mappings").with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("GET /actuator/metrics is rejected with 401 for unauthenticated requests")
    void metricsEndpoint_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/actuator/metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /actuator/metrics is denied with 403 Forbidden even when authenticated")
    void metricsEndpoint_Authenticated_Returns403() throws Exception {
        Authentication auth = createAuthentication();
        mockMvc.perform(get("/actuator/metrics").with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
