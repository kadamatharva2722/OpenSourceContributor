package com.osc.backend.userpreference.controller;

import com.osc.backend.common.security.CurrentUserProvider;
import com.osc.backend.userpreference.dto.UpdateUserPreferenceRequestDto;
import com.osc.backend.userpreference.dto.UserPreferenceResponseDto;
import com.osc.backend.userpreference.service.UserPreferenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for managing authenticated user's discovery preferences.
 */
@RestController
@RequestMapping("/api/v1/preferences")
@Tag(name = "Preferences", description = "User discovery preferences operations")
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;
    private final CurrentUserProvider currentUserProvider;

    public UserPreferenceController(UserPreferenceService userPreferenceService, CurrentUserProvider currentUserProvider) {
        this.userPreferenceService = userPreferenceService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Retrieves preferences for the authenticated user.
     */
    @GetMapping
    @Operation(summary = "Get user preferences", description = "Retrieves discovery preferences for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferences retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Preferences not found")
    })
    public ResponseEntity<UserPreferenceResponseDto> getPreferences() {
        Long userId = currentUserProvider.getCurrentUserId();
        UserPreferenceResponseDto response = userPreferenceService.getPreferencesByUserId(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Creates or updates preferences for the authenticated user.
     */
    @PutMapping
    @Operation(summary = "Create or update user preferences", description = "Idempotently saves discovery preferences for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Preferences updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or difficulty"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    public ResponseEntity<UserPreferenceResponseDto> updatePreferences(
            @Valid @RequestBody UpdateUserPreferenceRequestDto request) {
        Long userId = currentUserProvider.getCurrentUserId();
        UserPreferenceResponseDto response = userPreferenceService.createOrUpdatePreferences(userId, request);
        return ResponseEntity.ok(response);
    }
}
