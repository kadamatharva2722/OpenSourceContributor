package com.osc.backend.aiusage.controller;

import com.osc.backend.aiusage.dto.AiQuotaResponseDto;
import com.osc.backend.aiusage.service.AiUsageService;
import com.osc.backend.common.security.CurrentUserProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for inspecting AI discovery quota consumption and remaining queries.
 * Strictly read-only; does not expose arbitrary consume endpoints.
 */
@RestController
@RequestMapping("/api/v1/ai-usage")
@Tag(name = "AI Usage", description = "AI discovery quota metrics")
public class AiUsageController {

    private final AiUsageService aiUsageService;
    private final CurrentUserProvider currentUserProvider;

    public AiUsageController(AiUsageService aiUsageService, CurrentUserProvider currentUserProvider) {
        this.aiUsageService = aiUsageService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Retrieves today's AI discovery quota metrics for the authenticated user without consuming quota.
     */
    @GetMapping
    @Operation(summary = "Get AI quota usage", description = "Retrieves today's AI search usage metrics and remaining searches")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI quota metrics retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required")
    })
    public ResponseEntity<AiQuotaResponseDto> getAiUsage() {
        Long userId = currentUserProvider.getCurrentUserId();
        AiQuotaResponseDto quotaInfo = aiUsageService.getQuotaInfo(userId);
        return ResponseEntity.ok(quotaInfo);
    }
}
