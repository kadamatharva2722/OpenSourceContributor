package com.osc.backend.account.service;

import com.osc.backend.aiusage.repository.AiUsageRepository;
import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.common.exception.ReauthenticationRequiredException;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.common.security.AppUserPrincipal;
import com.osc.backend.contribution.repository.ContributionRepository;
import com.osc.backend.savedissue.repository.SavedIssueRepository;
import com.osc.backend.user.entity.User;
import com.osc.backend.user.repository.UserRepository;
import com.osc.backend.userpreference.repository.UserPreferenceRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private SavedIssueRepository savedIssueRepository;

    @Mock
    private ContributionRepository contributionRepository;

    @Mock
    private AiUsageRepository aiUsageRepository;

    @Mock
    private OAuth2AuthorizedClientRepository authorizedClientRepository;

    @Mock
    private SessionRegistry sessionRegistry;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(
                userRepository,
                userPreferenceRepository,
                savedIssueRepository,
                contributionRepository,
                aiUsageRepository,
                authorizedClientRepository,
                sessionRegistry,
                30L
        );
    }

    @Test
    @DisplayName("deleteAccount should transactionally delete user and all dependent records")
    void deleteAccount_Success() {
        Long userId = 1L;
        User user = new User(12345L, "testuser", "Test User", "https://avatar.url", "bio");
        user.setId(userId);

        AppUserPrincipal principal = new AppUserPrincipal(userId, 12345L, "testuser", Collections.emptyMap(), Collections.emptyList(), Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authentication.getPrincipal()).thenReturn(principal);
        when(request.getSession(false)).thenReturn(session);

        accountService.deleteAccount(userId, authentication, request, response);

        // Verify dependent records are deleted for this user
        verify(userPreferenceRepository).deleteByUserId(userId);
        verify(savedIssueRepository).deleteByUserId(userId);
        verify(contributionRepository).deleteByUserId(userId);
        verify(aiUsageRepository).deleteByUserId(userId);

        // Verify user entity is deleted and flushed
        verify(userRepository).delete(user);
        verify(userRepository).flush();

        // Verify session invalidation and cookie expiration
        verify(session).invalidate();
        ArgumentCaptor<Cookie> cookieCaptor = ArgumentCaptor.forClass(Cookie.class);
        verify(response).addCookie(cookieCaptor.capture());
        Cookie expiredCookie = cookieCaptor.getValue();
        assertThat(expiredCookie.getName()).isEqualTo("JSESSIONID");
        assertThat(expiredCookie.getMaxAge()).isEqualTo(0);
    }

    @Test
    @DisplayName("deleteAccount must enforce ownership isolation and only delete records for target userId")
    void deleteAccount_OwnershipIsolation() {
        Long authenticatedUserId = 42L;
        Long otherUserId = 99L;

        User targetUser = new User(4242L, "user42", "User 42", null, null);
        targetUser.setId(authenticatedUserId);

        AppUserPrincipal principal = new AppUserPrincipal(authenticatedUserId, 4242L, "user42", Collections.emptyMap(), Collections.emptyList(), Instant.now());
        when(userRepository.findById(authenticatedUserId)).thenReturn(Optional.of(targetUser));
        when(authentication.getPrincipal()).thenReturn(principal);

        accountService.deleteAccount(authenticatedUserId, authentication, request, response);

        // Verify only user 42 was targeted
        verify(userPreferenceRepository).deleteByUserId(authenticatedUserId);
        verify(savedIssueRepository).deleteByUserId(authenticatedUserId);
        verify(contributionRepository).deleteByUserId(authenticatedUserId);
        verify(aiUsageRepository).deleteByUserId(authenticatedUserId);
        verify(userRepository).delete(targetUser);

        // Verify other user records were never touched
        verify(userPreferenceRepository, never()).deleteByUserId(otherUserId);
        verify(savedIssueRepository, never()).deleteByUserId(otherUserId);
        verify(contributionRepository, never()).deleteByUserId(otherUserId);
        verify(aiUsageRepository, never()).deleteByUserId(otherUserId);
    }

    @Test
    @DisplayName("deleteAccount should throw ReauthenticationRequiredException when authentication is stale")
    void deleteAccount_ReauthenticationRequired() {
        Long userId = 1L;
        User user = new User(12345L, "testuser", "Test User", null, null);
        user.setId(userId);

        // Authenticated 2 hours ago (exceeds 30 minute window)
        Instant staleAuthTime = Instant.now().minus(2, ChronoUnit.HOURS);
        AppUserPrincipal stalePrincipal = new AppUserPrincipal(userId, 12345L, "testuser", Collections.emptyMap(), Collections.emptyList(), staleAuthTime);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authentication.getPrincipal()).thenReturn(stalePrincipal);

        assertThatThrownBy(() -> accountService.deleteAccount(userId, authentication, request, response))
                .isInstanceOf(ReauthenticationRequiredException.class)
                .hasMessageContaining("Recent authentication is required");

        // Verify no deletions occurred
        verify(userPreferenceRepository, never()).deleteByUserId(any());
        verify(savedIssueRepository, never()).deleteByUserId(any());
        verify(contributionRepository, never()).deleteByUserId(any());
        verify(aiUsageRepository, never()).deleteByUserId(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteAccount should throw ResourceNotFoundException when user does not exist")
    void deleteAccount_UserNotFound() {
        Long nonExistentId = 999L;
        when(userRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.deleteAccount(nonExistentId, authentication, request, response))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(userPreferenceRepository, never()).deleteByUserId(any());
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteAccount should throw InvalidRequestException when userId is null")
    void deleteAccount_NullUserId() {
        assertThatThrownBy(() -> accountService.deleteAccount(null, authentication, request, response))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("User ID cannot be null");
    }

    @Test
    @DisplayName("deleteAccount should invalidate all sessions in SessionRegistry for deleted user")
    void deleteAccount_SessionRegistryInvalidation() {
        Long userId = 7L;
        User user = new User(777L, "user7", "User Seven", null, null);
        user.setId(userId);

        AppUserPrincipal principal = new AppUserPrincipal(userId, 777L, "user7", Collections.emptyMap(), Collections.emptyList(), Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authentication.getPrincipal()).thenReturn(principal);

        SessionInformation sessionInfo = mock(SessionInformation.class);
        when(sessionRegistry.getAllPrincipals()).thenReturn(List.of(principal));
        when(sessionRegistry.getAllSessions(principal, false)).thenReturn(List.of(sessionInfo));

        accountService.deleteAccount(userId, authentication, request, response);

        verify(sessionInfo).expireNow();
    }

    @Test
    @DisplayName("deleteAccount should clear session authorized client from repository when present")
    void deleteAccount_ClearSessionAuthorizedClient() {
        Long userId = 1L;
        User user = new User(12345L, "testuser", "Test User", null, null);
        user.setId(userId);

        AppUserPrincipal principal = new AppUserPrincipal(userId, 12345L, "testuser", Collections.emptyMap(), Collections.emptyList(), Instant.now());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(authentication.getPrincipal()).thenReturn(principal);

        accountService.deleteAccount(userId, authentication, request, response);

        verify(authorizedClientRepository).removeAuthorizedClient(eq("github"), eq(authentication), eq(request), eq(response));
    }
}
