package com.osc.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.osc.backend.common.dto.ErrorResponseDto;
import com.osc.backend.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAccountValidationFilterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FilterChain filterChain;

    private ObjectMapper objectMapper;
    private UserAccountValidationFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        filter = new UserAccountValidationFilter(userRepository, objectMapper);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateUser(Long userId, Long githubId, String username) {
        AppUserPrincipal principal = new AppUserPrincipal(userId, githubId, username);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, Collections.emptyList());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Active authenticated user passes validation and filter chain continues")
    void doFilterInternal_WhenUserExistsInDatabase_ContinuesFilterChain() throws Exception {
        authenticateUser(100L, 12345L, "alice");
        when(userRepository.existsById(100L)).thenReturn(true);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userRepository).existsById(100L);
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    }

    @Test
    @DisplayName("Deleted user account returns HTTP 401, clears SecurityContext, invalidates session and halts filter chain")
    void doFilterInternal_WhenUserDoesNotExistInDatabase_TerminatesSessionWith401() throws Exception {
        authenticateUser(200L, 67890L, "deleteduser");
        when(userRepository.existsById(200L)).thenReturn(false);

        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setRequestURI("/api/v1/saved-issues");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(any(), any());
        assertThat(response.getStatus()).isEqualTo(401);

        ErrorResponseDto errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponseDto.class);
        assertThat(errorResponse.getStatus()).isEqualTo(401);
        assertThat(errorResponse.getError()).isEqualTo("Unauthorized");
        assertThat(errorResponse.getMessage()).isEqualTo("User account has been deleted or no longer exists");
        assertThat(errorResponse.getPath()).isEqualTo("/api/v1/saved-issues");

        assertThat(session.isInvalid()).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();

        Cookie cookie = response.getCookie("JSESSIONID");
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isEqualTo(0);
        assertThat(cookie.isHttpOnly()).isTrue();
    }

    @Test
    @DisplayName("Database failure returns HTTP 503 Service Unavailable without invalidating session or clearing SecurityContext")
    void doFilterInternal_WhenDatabaseFails_Returns503WithoutInvalidatingSession() throws Exception {
        authenticateUser(300L, 99999L, "dbuser");
        when(userRepository.existsById(300L)).thenThrow(new DataAccessResourceFailureException("PostgreSQL connection timeout"));

        MockHttpSession session = new MockHttpSession();
        request.setSession(session);
        request.setRequestURI("/api/v1/contributions");

        filter.doFilter(request, response, filterChain);

        // Filter chain halts
        verify(filterChain, never()).doFilter(any(), any());

        // HTTP 503 returned
        assertThat(response.getStatus()).isEqualTo(503);

        ErrorResponseDto errorResponse = objectMapper.readValue(response.getContentAsString(), ErrorResponseDto.class);
        assertThat(errorResponse.getStatus()).isEqualTo(503);
        assertThat(errorResponse.getError()).isEqualTo("Service Unavailable");
        assertThat(errorResponse.getMessage()).isEqualTo("Authentication verification temporarily unavailable. Please try again.");
        assertThat(errorResponse.getPath()).isEqualTo("/api/v1/contributions");

        // CRITICAL: Session is NOT invalidated
        assertThat(session.isInvalid()).isFalse();

        // CRITICAL: SecurityContext is NOT cleared
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();

        // Expired JSESSIONID cookie is NOT set
        assertThat(response.getCookie("JSESSIONID")).isNull();
    }

    @Test
    @DisplayName("Unauthenticated request bypasses filter without database queries")
    void doFilterInternal_WhenUnauthenticated_BypassesFilterWithoutDbQueries() throws Exception {
        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("AnonymousAuthenticationToken request bypasses filter without database queries")
    void doFilterInternal_WhenAnonymousAuthentication_BypassesFilterWithoutDbQueries() throws Exception {
        AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
                "anonymousKey", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS")
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(anonymous);
        SecurityContextHolder.setContext(context);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("Principal not instance of AppUserPrincipal bypasses filter without database queries")
    void doFilterInternal_WhenPrincipalNotAppUserPrincipal_BypassesFilterWithoutDbQueries() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken("plain-string-principal", null, Collections.emptyList());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("AppUserPrincipal with null userId bypasses filter without database queries")
    void doFilterInternal_WhenPrincipalUserIdIsNull_BypassesFilterWithoutDbQueries() throws Exception {
        authenticateUser(null, 12345L, "nouserid");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(userRepository, never()).existsById(any());
    }
}
