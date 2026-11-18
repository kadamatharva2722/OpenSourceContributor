package com.osc.backend.discovery.controller;

import com.osc.backend.common.security.ClientIpRateLimiter;
import com.osc.backend.common.security.ClientIpResolver;
import com.osc.backend.discovery.dto.DiscoverySearchRequestDto;
import com.osc.backend.discovery.dto.DiscoverySearchResponseDto;
import com.osc.backend.discovery.dto.PublicDiscoveryRequestDto;
import com.osc.backend.discovery.service.DiscoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for GitHub issue discovery.
 * Provides public anonymous issue browsing and authenticated AI-powered semantic search.
 */
@RestController
@RequestMapping("/api/v1/discovery")
@Tag(name = "Discovery", description = "Open source issue discovery engine (Guest browsing & Authenticated AI)")
public class DiscoveryController {

    private final DiscoveryService discoveryService;
    private final ClientIpResolver clientIpResolver;
    private final ClientIpRateLimiter clientIpRateLimiter;

    @Autowired
    public DiscoveryController(
            DiscoveryService discoveryService,
            ClientIpResolver clientIpResolver,
            ClientIpRateLimiter clientIpRateLimiter
    ) {
        this.discoveryService = discoveryService;
        this.clientIpResolver = clientIpResolver;
        this.clientIpRateLimiter = clientIpRateLimiter;
    }

    public DiscoveryController(DiscoveryService discoveryService) {
        this(discoveryService, new ClientIpResolver(""), new ClientIpRateLimiter());
    }

    /**
     * Public, unauthenticated GitHub issue discovery using explicit filters.
     * Bypasses AI inference and daily user quotas. Protected by application-level client IP rate limiting.
     *
     * @param request search query parameters
     * @param httpRequest current HTTP request for client IP resolution
     * @param httpResponse current HTTP response for rate limit header propagation
     * @return search response containing verified GitHub issues and factual criteria
     */
    @GetMapping("/public")
    @Operation(
            summary = "Discover public GitHub issues as guest",
            description = "Public issue browsing using explicit filters. Bypasses AI inference and user quotas. Rate-limited by client IP."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Public issues discovered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters or no filter provided"),
            @ApiResponse(responseCode = "429", description = "Client IP rate limit exceeded or GitHub rate limit reached"),
            @ApiResponse(responseCode = "502", description = "GitHub API communication or invalid response error"),
            @ApiResponse(responseCode = "503", description = "GitHub service temporarily unavailable")
    })
    public ResponseEntity<DiscoverySearchResponseDto> browsePublicIssues(
            @Valid @ModelAttribute PublicDiscoveryRequestDto request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        String clientIp = clientIpResolver.resolveClientIp(httpRequest);

        // Attach application rate limit headers
        ClientIpRateLimiter.RateLimitInfo info = clientIpRateLimiter.getRateLimitInfo(clientIp);
        httpResponse.setHeader("X-RateLimit-Limit", String.valueOf(info.limit()));
        httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(info.remaining()));
        httpResponse.setHeader("X-RateLimit-Reset", String.valueOf(info.resetEpochSeconds()));

        DiscoverySearchResponseDto response = discoveryService.browsePublicIssues(request, clientIp);
        return ResponseEntity.ok(response);
    }

    /**
     * Executes AI-powered issue discovery for the authenticated contributor.
     * Consumes 1 daily AI search quota (out of 20) in PostgreSQL.
     *
     * @param request search request with natural language prompt and pagination options
     * @return search response containing interpreted criteria, real GitHub issues, and match explanations
     */
    @PostMapping("/search")
    @Operation(
            summary = "Discover GitHub issues using natural language (Authenticated)",
            description = "Interprets natural language queries using AI (Groq), queries the public GitHub REST API, and returns matching issues with verified factual explanations."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Issues discovered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid search prompt or pagination parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "429", description = "AI daily search quota exceeded or GitHub rate limit reached"),
            @ApiResponse(responseCode = "502", description = "GitHub API communication or invalid response error"),
            @ApiResponse(responseCode = "503", description = "AI discovery service or GitHub temporarily unavailable")
    })
    public ResponseEntity<DiscoverySearchResponseDto> searchIssues(
            @Valid @RequestBody DiscoverySearchRequestDto request
    ) {
        DiscoverySearchResponseDto response = discoveryService.discoverIssues(request);
        return ResponseEntity.ok(response);
    }
}
