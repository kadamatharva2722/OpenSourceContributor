package com.osc.backend.discovery.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.common.exception.AiQuotaExceededException;
import com.osc.backend.common.exception.ClientRateLimitExceededException;
import com.osc.backend.common.exception.GlobalExceptionHandler;
import com.osc.backend.common.exception.UnauthenticatedException;
import com.osc.backend.common.security.ClientIpRateLimiter;
import com.osc.backend.common.security.ClientIpResolver;
import com.osc.backend.discovery.dto.DiscoverySearchRequestDto;
import com.osc.backend.discovery.dto.DiscoverySearchResponseDto;
import com.osc.backend.discovery.dto.ParsedFilterDto;
import com.osc.backend.discovery.dto.PublicDiscoveryRequestDto;
import com.osc.backend.discovery.exception.AiServiceUnavailableException;
import com.osc.backend.discovery.service.DiscoveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DiscoveryController Unit Tests")
class DiscoveryControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private DiscoveryService discoveryService;

    @Spy
    private ClientIpResolver clientIpResolver = new ClientIpResolver("");

    @Spy
    private ClientIpRateLimiter clientIpRateLimiter = new ClientIpRateLimiter();

    @InjectMocks
    private DiscoveryController discoveryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(discoveryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/discovery/search returns 200 with search results")
    void searchIssues_Success_Returns200() throws Exception {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find beginner Java issues", 1, 30);

        ParsedFilterDto filter = new ParsedFilterDto(null, "Java", null, List.of("good first issue"), "open", null, null, "beginner", 1, 30);
        DiscoverySearchResponseDto response = new DiscoverySearchResponseDto(
                request.getPrompt(), filter, Collections.emptyList(), 0L, 1, 30, false, false, 19, null, "AUTHENTICATED_AI"
        );

        when(discoveryService.discoverIssues(any(DiscoverySearchRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/discovery/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.prompt").value("Find beginner Java issues"))
                .andExpect(jsonPath("$.remainingDailyQuota").value(19))
                .andExpect(jsonPath("$.discoveryMode").value("AUTHENTICATED_AI"))
                .andExpect(jsonPath("$.accessibleCount").value(0))
                .andExpect(jsonPath("$.totalPages").value(0))
                .andExpect(jsonPath("$.parsedFilter.language").value("Java"));
    }

    @Test
    @DisplayName("POST /api/v1/discovery/search returns 400 when prompt is blank")
    void searchIssues_BlankPrompt_Returns400() throws Exception {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("   ", 1, 30);

        mockMvc.perform(post("/api/v1/discovery/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/discovery/search returns 429 when quota is exceeded")
    void searchIssues_QuotaExceeded_Returns429() throws Exception {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find beginner Python issues");

        when(discoveryService.discoverIssues(any(DiscoverySearchRequestDto.class)))
                .thenThrow(new AiQuotaExceededException("Daily AI search quota exhausted"));

        mockMvc.perform(post("/api/v1/discovery/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Daily AI search quota exhausted"));
    }

    @Test
    @DisplayName("POST /api/v1/discovery/search returns 503 when AI service is unavailable")
    void searchIssues_AiUnavailable_Returns503() throws Exception {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find beginner Rust issues");

        when(discoveryService.discoverIssues(any(DiscoverySearchRequestDto.class)))
                .thenThrow(new AiServiceUnavailableException("AI discovery engine is currently unavailable"));

        mockMvc.perform(post("/api/v1/discovery/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.message").value("AI discovery engine is currently unavailable"));
    }

    @Test
    @DisplayName("POST /api/v1/discovery/search returns 401 when user is not authenticated")
    void searchIssues_Unauthenticated_Returns401() throws Exception {
        DiscoverySearchRequestDto request = new DiscoverySearchRequestDto("Find beginner Java issues");

        when(discoveryService.discoverIssues(any(DiscoverySearchRequestDto.class)))
                .thenThrow(new UnauthenticatedException("Authentication is required to access this resource"));

        mockMvc.perform(post("/api/v1/discovery/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"));
    }

    @Test
    @DisplayName("GET /api/v1/discovery/public returns 200 with guest discovery results")
    void browsePublicIssues_Success_Returns200() throws Exception {
        ParsedFilterDto filter = new ParsedFilterDto("spring", "Java", null, List.of("good first issue"), "open", "updated", "desc", null, 1, 30);
        DiscoverySearchResponseDto response = new DiscoverySearchResponseDto(
                null, filter, Collections.emptyList(), 5L, 1, 30, false, false, null, null, "GUEST"
        );

        when(discoveryService.browsePublicIssues(any(PublicDiscoveryRequestDto.class), any(String.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/discovery/public")
                        .param("query", "spring")
                        .param("language", "Java")
                        .param("page", "1")
                        .param("perPage", "30"))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-RateLimit-Limit"))
                .andExpect(header().exists("X-RateLimit-Remaining"))
                .andExpect(header().exists("X-RateLimit-Reset"))
                .andExpect(jsonPath("$.discoveryMode").value("GUEST"))
                .andExpect(jsonPath("$.prompt").doesNotExist())
                .andExpect(jsonPath("$.remainingDailyQuota").doesNotExist())
                .andExpect(jsonPath("$.parsedFilter.language").value("Java"));
    }

    @Test
    @DisplayName("GET /api/v1/discovery/public returns 429 when client IP rate limit exceeded")
    void browsePublicIssues_RateLimitExceeded_Returns429() throws Exception {
        when(discoveryService.browsePublicIssues(any(PublicDiscoveryRequestDto.class), any(String.class)))
                .thenThrow(new ClientRateLimitExceededException("Public discovery rate limit exceeded", 3600));

        mockMvc.perform(get("/api/v1/discovery/public")
                        .param("query", "kafka"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "3600"))
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("Public discovery rate limit exceeded"));
    }
}
