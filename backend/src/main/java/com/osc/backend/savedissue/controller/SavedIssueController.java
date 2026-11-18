package com.osc.backend.savedissue.controller;

import com.osc.backend.common.security.CurrentUserProvider;
import com.osc.backend.savedissue.dto.SaveIssueRequestDto;
import com.osc.backend.savedissue.dto.SavedIssueResponseDto;
import com.osc.backend.savedissue.service.SavedIssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Controller for managing bookmarked / saved GitHub issues.
 */
@RestController
@RequestMapping("/api/v1/saved-issues")
@Tag(name = "Saved Issues", description = "Saved/bookmarked issues operations")
public class SavedIssueController {

    private final SavedIssueService savedIssueService;
    private final CurrentUserProvider currentUserProvider;

    public SavedIssueController(SavedIssueService savedIssueService, CurrentUserProvider currentUserProvider) {
        this.savedIssueService = savedIssueService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Saves an issue for the authenticated user.
     */
    @PostMapping
    @Operation(summary = "Save an issue", description = "Bookmarks a GitHub issue for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Issue saved successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "409", description = "Issue already saved by this user")
    })
    public ResponseEntity<SavedIssueResponseDto> saveIssue(@Valid @RequestBody SaveIssueRequestDto request) {
        Long userId = currentUserProvider.getCurrentUserId();
        SavedIssueResponseDto response = savedIssueService.saveIssue(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves all saved issues for the authenticated user.
     */
    @GetMapping
    @Operation(summary = "List saved issues", description = "Retrieves all bookmarked issues for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of saved issues"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    public ResponseEntity<List<SavedIssueResponseDto>> getSavedIssues() {
        Long userId = currentUserProvider.getCurrentUserId();
        List<SavedIssueResponseDto> response = savedIssueService.getSavedIssuesByUserId(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single saved issue by internal ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get saved issue by ID", description = "Retrieves a single saved issue ensuring user ownership")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saved issue retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Saved issue not found or not owned by user")
    })
    public ResponseEntity<SavedIssueResponseDto> getSavedIssueById(@PathVariable Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        SavedIssueResponseDto response = savedIssueService.getSavedIssueByIdAndUserId(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Checks whether a specific GitHub issue is saved by the authenticated user.
     */
    @GetMapping("/check/{githubIssueId}")
    @Operation(summary = "Check if issue is saved", description = "Checks if a GitHub issue ID is bookmarked by the user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Check status returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    public ResponseEntity<Map<String, Boolean>> checkSavedIssue(@PathVariable Long githubIssueId) {
        Long userId = currentUserProvider.getCurrentUserId();
        boolean saved = savedIssueService.isIssueSaved(userId, githubIssueId);
        return ResponseEntity.ok(Map.of("saved", saved));
    }

    /**
     * Removes a saved issue by internal ID.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete saved issue", description = "Removes a bookmarked issue for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Saved issue removed successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Saved issue not found or not owned by user")
    })
    public ResponseEntity<Void> removeSavedIssue(@PathVariable Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        savedIssueService.removeSavedIssue(id, userId);
        return ResponseEntity.noContent().build();
    }
}
