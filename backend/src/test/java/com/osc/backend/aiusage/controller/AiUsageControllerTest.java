package com.osc.backend.aiusage.controller;

import com.osc.backend.aiusage.dto.AiQuotaResponseDto;
import com.osc.backend.aiusage.service.AiUsageService;
import com.osc.backend.common.exception.GlobalExceptionHandler;
import com.osc.backend.common.exception.UnauthenticatedException;
import com.osc.backend.common.security.CurrentUserProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AiUsageControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AiUsageService aiUsageService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private AiUsageController aiUsageController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(aiUsageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/ai-usage should return 200 with quota info without consuming quota")
    void getAiUsage_Success() throws Exception {
        Long userId = 1L;
        LocalDate today = LocalDate.now();
        AiQuotaResponseDto quotaInfo = new AiQuotaResponseDto(20, 5, 15, today, false);

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(aiUsageService.getQuotaInfo(userId)).thenReturn(quotaInfo);

        mockMvc.perform(get("/api/v1/ai-usage")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyLimit").value(20))
                .andExpect(jsonPath("$.usedToday").value(5))
                .andExpect(jsonPath("$.remainingToday").value(15))
                .andExpect(jsonPath("$.quotaExceeded").value(false));

        verify(currentUserProvider).getCurrentUserId();
        verify(aiUsageService).getQuotaInfo(userId);
        // Strictly verify that consumeQuota is never invoked from read-only endpoint
        verify(aiUsageService, never()).consumeQuota(any());
    }

    @Test
    @DisplayName("GET /api/v1/ai-usage should return 401 when unauthenticated")
    void getAiUsage_Unauthenticated() throws Exception {
        when(currentUserProvider.getCurrentUserId())
                .thenThrow(new UnauthenticatedException("Authentication is required to access this resource"));

        mockMvc.perform(get("/api/v1/ai-usage")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/ai-usage"));

        verify(aiUsageService, never()).getQuotaInfo(any());
        verify(aiUsageService, never()).consumeQuota(any());
    }
}
