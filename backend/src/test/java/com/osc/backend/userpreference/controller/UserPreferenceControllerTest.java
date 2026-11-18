package com.osc.backend.userpreference.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.common.exception.GlobalExceptionHandler;
import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.common.exception.UnauthenticatedException;
import com.osc.backend.common.security.CurrentUserProvider;
import com.osc.backend.userpreference.dto.UpdateUserPreferenceRequestDto;
import com.osc.backend.userpreference.dto.UserPreferenceResponseDto;
import com.osc.backend.userpreference.service.UserPreferenceService;
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

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private UserPreferenceService userPreferenceService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserPreferenceController userPreferenceController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(userPreferenceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/preferences should return 200 with preferences for authenticated user")
    void getPreferences_Success() throws Exception {
        Long userId = 1L;
        UserPreferenceResponseDto responseDto = new UserPreferenceResponseDto(
                10L,
                userId,
                List.of("Java", "TypeScript"),
                List.of("Web", "DevOps"),
                "INTERMEDIATE",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userPreferenceService.getPreferencesByUserId(userId)).thenReturn(responseDto);

        mockMvc.perform(get("/api/v1/preferences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.preferredLanguages[0]").value("Java"))
                .andExpect(jsonPath("$.preferredLanguages[1]").value("TypeScript"))
                .andExpect(jsonPath("$.preferredDomains[0]").value("Web"))
                .andExpect(jsonPath("$.difficulty").value("INTERMEDIATE"));

        verify(currentUserProvider).getCurrentUserId();
        verify(userPreferenceService).getPreferencesByUserId(userId);
    }

    @Test
    @DisplayName("GET /api/v1/preferences should return 401 when unauthenticated")
    void getPreferences_Unauthenticated() throws Exception {
        when(currentUserProvider.getCurrentUserId())
                .thenThrow(new UnauthenticatedException("Authentication is required to access this resource"));

        mockMvc.perform(get("/api/v1/preferences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.path").value("/api/v1/preferences"));

        verify(userPreferenceService, never()).getPreferencesByUserId(any());
    }

    @Test
    @DisplayName("GET /api/v1/preferences should return 404 when preferences are not found")
    void getPreferences_NotFound() throws Exception {
        Long userId = 1L;
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userPreferenceService.getPreferencesByUserId(userId))
                .thenThrow(new ResourceNotFoundException("UserPreference", "userId", userId));

        mockMvc.perform(get("/api/v1/preferences")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("PUT /api/v1/preferences should update preferences and return 200")
    void updatePreferences_Success() throws Exception {
        Long userId = 1L;
        UpdateUserPreferenceRequestDto requestDto = new UpdateUserPreferenceRequestDto(
                List.of("Rust", "Go"),
                List.of("Systems"),
                "ADVANCED"
        );

        UserPreferenceResponseDto responseDto = new UserPreferenceResponseDto(
                10L,
                userId,
                List.of("Rust", "Go"),
                List.of("Systems"),
                "ADVANCED",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userPreferenceService.createOrUpdatePreferences(eq(userId), any(UpdateUserPreferenceRequestDto.class)))
                .thenReturn(responseDto);

        mockMvc.perform(put("/api/v1/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.difficulty").value("ADVANCED"))
                .andExpect(jsonPath("$.preferredLanguages[0]").value("Rust"));

        verify(userPreferenceService).createOrUpdatePreferences(eq(userId), any(UpdateUserPreferenceRequestDto.class));
    }

    @Test
    @DisplayName("PUT /api/v1/preferences should return 400 when service rejects difficulty value")
    void updatePreferences_InvalidDifficulty() throws Exception {
        Long userId = 1L;
        UpdateUserPreferenceRequestDto requestDto = new UpdateUserPreferenceRequestDto(
                List.of("Java"),
                List.of("Web"),
                "INVALID_DIFFICULTY"
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userPreferenceService.createOrUpdatePreferences(eq(userId), any(UpdateUserPreferenceRequestDto.class)))
                .thenThrow(new InvalidRequestException("Invalid difficulty 'INVALID_DIFFICULTY'. Allowed values: [BEGINNER, INTERMEDIATE, ADVANCED]"));

        mockMvc.perform(put("/api/v1/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid difficulty 'INVALID_DIFFICULTY'. Allowed values: [BEGINNER, INTERMEDIATE, ADVANCED]"));
    }
}
