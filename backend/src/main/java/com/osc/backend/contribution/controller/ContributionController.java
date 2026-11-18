package com.osc.backend.contribution.controller;

import com.osc.backend.common.security.CurrentUserProvider;
import com.osc.backend.contribution.dto.ContributionResponseDto;
import com.osc.backend.contribution.dto.CreateContributionRequestDto;
import com.osc.backend.contribution.dto.UpdateContributionRequestDto;
import com.osc.backend.contribution.entity.ContributionStatus;
import com.osc.backend.contribution.service.ContributionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller for tracking user open-source contributions.
 */
@RestController
@RequestMapping("/api/v1/contributions")
@Tag(name = "Contributions", description = "Contribution tracking operations")
public class ContributionController {

    private final ContributionService contributionService;
    private final CurrentUserProvider currentUserProvider;

    public ContributionController(ContributionService contributionService, CurrentUserProvider currentUserProvider) {
        this.contributionService = contributionService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Creates a new contribution record for the authenticated user.
     */
    @PostMapping
    @Operation(summary = "Create contribution", description = "Creates a new contribution tracking record for the user")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Contribution created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    public ResponseEntity<ContributionResponseDto> createContribution(
            @Valid @RequestBody CreateContributionRequestDto request) {
        Long userId = currentUserProvider.getCurrentUserId();
        ContributionResponseDto response = contributionService.createContribution(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Retrieves contributions for the authenticated user, optionally filtered by status.
     */
    @GetMapping
    @Operation(summary = "List contributions", description = "Retrieves contributions for the authenticated user with optional status filter")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of contributions"),
            @ApiResponse(responseCode = "400", description = "Invalid status enum parameter"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    public ResponseEntity<List<ContributionResponseDto>> getContributions(
            @RequestParam(required = false) ContributionStatus status) {
        Long userId = currentUserProvider.getCurrentUserId();
        List<ContributionResponseDto> response;
        if (status != null) {
            response = contributionService.getContributionsByUserIdAndStatus(userId, status);
        } else {
            response = contributionService.getContributionsByUserId(userId);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single contribution record by ID ensuring user ownership.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get contribution by ID", description = "Retrieves a contribution record ensuring ownership")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contribution retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Contribution not found or not owned by user")
    })
    public ResponseEntity<ContributionResponseDto> getContributionById(@PathVariable Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        ContributionResponseDto response = contributionService.getContributionByIdAndUserId(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Updates details (status, branchName, pullRequestUrl, notes) for a contribution.
     */
    @PatchMapping("/{id}")
    @Operation(summary = "Update contribution", description = "Updates details for an existing contribution record")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contribution updated successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed for request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Contribution not found or not owned by user")
    })
    public ResponseEntity<ContributionResponseDto> updateContribution(
            @PathVariable Long id,
            @Valid @RequestBody UpdateContributionRequestDto request) {
        Long userId = currentUserProvider.getCurrentUserId();
        ContributionResponseDto response = contributionService.updateContributionDetails(id, userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Deletes a contribution record by ID ensuring user ownership.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete contribution", description = "Removes a contribution record for the authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Contribution deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "Contribution not found or not owned by user")
    })
    public ResponseEntity<Void> deleteContribution(@PathVariable Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        contributionService.deleteContribution(id, userId);
        return ResponseEntity.noContent().build();
    }
}
