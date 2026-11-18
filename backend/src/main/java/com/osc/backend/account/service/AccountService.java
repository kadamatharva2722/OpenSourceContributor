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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service managing transactional account deletion, cascade purges of associated user data,
 * session invalidation, and session-scoped OAuth credential cleanup.
 */
@Service
public class AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountService.class);

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final SavedIssueRepository savedIssueRepository;
    private final ContributionRepository contributionRepository;
    private final AiUsageRepository aiUsageRepository;
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final SessionRegistry sessionRegistry;
    private final long reauthWindowMinutes;

    private final Set<Long> inProgressDeletions = ConcurrentHashMap.newKeySet();

    public AccountService(
            UserRepository userRepository,
            UserPreferenceRepository userPreferenceRepository,
            SavedIssueRepository savedIssueRepository,
            ContributionRepository contributionRepository,
            AiUsageRepository aiUsageRepository,
            @Autowired(required = false) OAuth2AuthorizedClientRepository authorizedClientRepository,
            @Autowired(required = false) SessionRegistry sessionRegistry,
            @Value("${app.security.reauth-window-minutes:30}") long reauthWindowMinutes
    ) {
        this.userRepository = userRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.savedIssueRepository = savedIssueRepository;
        this.contributionRepository = contributionRepository;
        this.aiUsageRepository = aiUsageRepository;
        this.authorizedClientRepository = authorizedClientRepository;
        this.sessionRegistry = sessionRegistry;
        this.reauthWindowMinutes = reauthWindowMinutes;
    }

    /**
     * Executes permanent account deletion for the specified user ID derived strictly from the authenticated session.
     * Transactionally purges preferences, saved issues, contributions, and AI usage records,
     * then cleans up session-scoped credentials and invalidates active sessions.
     *
     * @param userId         authenticated user's database ID
     * @param authentication active Spring Security authentication object
     * @param request        current HTTP request for session cleanup
     * @param response       current HTTP response for cookie invalidation
     */
    public void deleteAccount(
            Long userId,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (userId == null) {
            throw new InvalidRequestException("User ID cannot be null");
        }

        // Prevent duplicate concurrent deletion requests for the same user
        if (!inProgressDeletions.add(userId)) {
            throw new InvalidRequestException("An account deletion request is already being processed");
        }

        try {
            // 1. Verify existence of the user in database
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

            // 2. Enforce recent authentication check
            validateRecentAuthentication(authentication);

            // 3. Transactionally purge all user records and user account (atomic rollback on any failure)
            purgeUserDatabaseRecords(userId, user);

            // 4. Remove session-bound OAuth client registration if present
            clearSessionAuthorizedClient(authentication, request, response);

            // 5. Invalidate all active sessions for this user across SessionRegistry and HTTP session
            invalidateSessions(userId, request, response);

        } finally {
            inProgressDeletions.remove(userId);
        }
    }

    /**
     * Atomically executes database deletion strictly within a database transaction boundary.
     * Ensures all dependent entities and the parent user row are purged together.
     * If an error occurs, the transaction rolls back cleanly without leaving the account partially deleted.
     */
    @Transactional
    public void purgeUserDatabaseRecords(Long userId, User user) {
        log.info("Executing transactional data purge for user id={}, username='{}'", userId, user.getUsername());
        userPreferenceRepository.deleteByUserId(userId);
        savedIssueRepository.deleteByUserId(userId);
        contributionRepository.deleteByUserId(userId);
        aiUsageRepository.deleteByUserId(userId);
        userRepository.delete(user);
        userRepository.flush();
        log.info("Successfully deleted user id={} and all associated records", userId);
    }

    /**
     * Verifies that the user authenticated within the configured re-authentication window.
     */
    private void validateRecentAuthentication(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
            return;
        }

        Instant authTime = principal.getAuthenticatedAt();
        if (authTime == null || Duration.between(authTime, Instant.now()).toMinutes() > reauthWindowMinutes) {
            log.warn("Re-authentication required: principal authTime={}, reauthWindowMinutes={}",
                    authTime, reauthWindowMinutes);
            throw new ReauthenticationRequiredException(
                    "Recent authentication is required to delete your account. Please sign in again."
            );
        }
    }

    /**
     * Clears ephemeral session-bound OAuth authorized client from repository if present.
     * The application never retains GitHub OAuth tokens in persistent storage.
     */
    private void clearSessionAuthorizedClient(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (authorizedClientRepository == null || authentication == null || request == null || response == null) {
            return;
        }

        try {
            authorizedClientRepository.removeAuthorizedClient("github", authentication, request, response);
        } catch (Exception ex) {
            log.warn("Non-fatal error clearing session OAuth client: {}", ex.getMessage());
        }
    }

    /**
     * Terminates all active sessions for the user across the system.
     */
    private void invalidateSessions(Long userId, HttpServletRequest request, HttpServletResponse response) {
        // Expire all registered sessions in Spring Security SessionRegistry
        if (sessionRegistry != null) {
            try {
                for (Object principal : sessionRegistry.getAllPrincipals()) {
                    if (principal instanceof AppUserPrincipal appPrincipal && userId.equals(appPrincipal.getUserId())) {
                        List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                        for (SessionInformation sessionInfo : sessions) {
                            sessionInfo.expireNow();
                        }
                    }
                }
            } catch (Exception ex) {
                log.warn("Error invalidating sessions in SessionRegistry: {}", ex.getMessage());
            }
        }

        // Invalidate current HTTP session
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
        }

        // Clear security context
        SecurityContextHolder.clearContext();

        // Expire session cookie
        if (response != null) {
            Cookie jsessionCookie = new Cookie("JSESSIONID", "");
            jsessionCookie.setPath("/");
            jsessionCookie.setMaxAge(0);
            jsessionCookie.setHttpOnly(true);
            response.addCookie(jsessionCookie);
        }
    }
}
