package com.osc.backend.discovery.service;

import com.osc.backend.discovery.dto.DiscoverySearchRequestDto;
import com.osc.backend.discovery.dto.DiscoverySearchResponseDto;
import com.osc.backend.discovery.dto.PublicDiscoveryRequestDto;

/**
 * Service contract for GitHub issue discovery.
 * Supports both authenticated AI discovery and public guest browsing.
 */
public interface DiscoveryService {

    /**
     * Interprets natural language contributor prompt using AI and discovers matching
     * public GitHub issues with verified factual match explanations and multi-factor ranking.
     * Consumes exactly 1 daily quota from the authenticated contributor.
     *
     * @param request search request with natural language prompt and pagination
     * @return response with parsed criteria, matched issues, pagination, and remaining quota
     */
    DiscoverySearchResponseDto discoverIssues(DiscoverySearchRequestDto request);

    /**
     * Public, unauthenticated GitHub issue discovery using explicit filters.
     * Bypasses Groq LLM, AiUsageService quota, and CurrentUserProvider completely.
     * Applies in-memory client IP rate limiting and verifies repository primary languages.
     *
     * @param request public discovery request with explicit filter criteria
     * @param clientIp client IP address for application rate limiting
     * @return response with matching issues, factual explanations, and null matchScore
     */
    DiscoverySearchResponseDto browsePublicIssues(PublicDiscoveryRequestDto request, String clientIp);
}
