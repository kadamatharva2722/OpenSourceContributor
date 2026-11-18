package com.osc.backend.account.controller;

import com.osc.backend.account.service.AccountService;
import com.osc.backend.common.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller for managing authenticated account lifecycle and deletion operations.
 * Enforces strict session-derived identity and prevents user ID injection from clients.
 */
@RestController
@RequestMapping("/api/v1/account")
@Tag(name = "Account", description = "Account lifecycle and data deletion operations")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUserProvider currentUserProvider;

    public AccountController(AccountService accountService, CurrentUserProvider currentUserProvider) {
        this.accountService = accountService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Permanently deletes the authenticated user's account and all associated workspace data.
     * Identity is resolved strictly from the authenticated security session.
     */
    @DeleteMapping
    @Operation(
            summary = "Delete My Account",
            description = "Permanently deletes the authenticated contributor's account, preferences, saved issues, contributions, and AI usage records."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account successfully deleted"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "403", description = "Forbidden - CSRF invalid or fresh re-authentication required"),
            @ApiResponse(responseCode = "404", description = "User account not found")
    })
    public ResponseEntity<Map<String, String>> deleteAccount(
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserId();
        accountService.deleteAccount(currentUserId, authentication, request, response);
        return ResponseEntity.ok(Map.of("message", "Account and associated data deleted successfully"));
    }
}
