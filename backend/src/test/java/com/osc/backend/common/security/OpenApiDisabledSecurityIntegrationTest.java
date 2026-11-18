package com.osc.backend.common.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import com.osc.backend.user.service.UserService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@Import({SecurityConfig.class, SecurityContextCurrentUserProvider.class, OAuth2ClientConfig.class})
@TestPropertySource(properties = {
        "app.openapi.enabled=false",
        "springdoc.api-docs.enabled=false",
        "springdoc.swagger-ui.enabled=false"
})
class OpenApiDisabledSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

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
    @DisplayName("When app.openapi.enabled=false, unauthenticated /v3/api-docs is rejected with 401")
    void openApiEndpoints_WhenDisabled_Unauthenticated_Returns401() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }

    @Test
    @DisplayName("When app.openapi.enabled=false, authenticated /v3/api-docs is denied with 403 Forbidden")
    void openApiEndpoints_WhenDisabled_Authenticated_Returns403() throws Exception {
        Authentication auth = createAuthentication();
        mockMvc.perform(get("/v3/api-docs").with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }

    @Test
    @DisplayName("When app.openapi.enabled=false, authenticated /swagger-ui/index.html is denied with 403 Forbidden")
    void swaggerUi_WhenDisabled_Authenticated_Returns403() throws Exception {
        Authentication auth = createAuthentication();
        mockMvc.perform(get("/swagger-ui/index.html").with(authentication(auth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
