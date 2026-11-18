package com.osc.backend.system.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lightweight system controller providing public operational ping and wake-up endpoints.
 * Explicitly designed with zero database, AI, external API, or business logic dependencies
 * to wake sleeping cloud instances (e.g. Render Free Web Services) immediately on visitor entry.
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "System", description = "System operational and instance readiness endpoints")
public class SystemController {

    /**
     * Lightweight wake-up endpoint.
     * Returns HTTP 204 No Content immediately when the application context is active.
     * Performs no I/O, no database queries, and accesses no external services.
     *
     * @return 204 No Content response
     */
    @GetMapping("/wake")
    @Operation(
            summary = "Wake backend service",
            description = "Lightweight unauthenticated endpoint used to wake sleeping instances without triggering database queries, AI, or external API calls."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Backend is awake and ready to accept requests")
    })
    public ResponseEntity<Void> wake() {
        return ResponseEntity.noContent().build();
    }
}
