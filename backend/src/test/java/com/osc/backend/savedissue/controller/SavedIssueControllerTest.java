package com.osc.backend.savedissue.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.common.exception.DuplicateResourceException;
import com.osc.backend.common.exception.GlobalExceptionHandler;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.common.exception.UnauthenticatedException;
import com.osc.backend.common.security.CurrentUserProvider;
import com.osc.backend.savedissue.dto.SaveIssueRequestDto;
import com.osc.backend.savedissue.dto.SavedIssueResponseDto;
import com.osc.backend.savedissue.service.SavedIssueService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SavedIssueControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private SavedIssueService savedIssueService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private SavedIssueController savedIssueController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(savedIssueController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/saved-issues should save issue and return 201 Created")
    void saveIssue_Success() throws Exception {
        Long userId = 1L;
        SaveIssueRequestDto request = new SaveIssueRequestDto(
                9999L,
                "facebook/react",
                42,
                "Bug: Hook memoization issue",
                "https://github.com/facebook/react/issues/42"
        );

        SavedIssueResponseDto response = new SavedIssueResponseDto(
                100L,
                userId,
                9999L,
                "facebook/react",
                42,
                "Bug: Hook memoization issue",
                "https://github.com/facebook/react/issues/42",
                OffsetDateTime.now()
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(savedIssueService.saveIssue(eq(userId), any(SaveIssueRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/saved-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.githubIssueId").value(9999))
                .andExpect(jsonPath("$.repositoryFullName").value("facebook/react"))
                .andExpect(jsonPath("$.issueNumber").value(42))
                .andExpect(jsonPath("$.title").value("Bug: Hook memoization issue"));

        verify(savedIssueService).saveIssue(eq(userId), any(SaveIssueRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/saved-issues should return 400 Bad Request when required fields are missing")
    void saveIssue_ValidationFailure() throws Exception {
        // Missing githubIssueId and repositoryFullName
        SaveIssueRequestDto request = new SaveIssueRequestDto();
        request.setTitle("Invalid Issue");

        mockMvc.perform(post("/api/v1/saved-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.path").value("/api/v1/saved-issues"));

        verify(savedIssueService, never()).saveIssue(any(), any());
    }

    @Test
    @DisplayName("POST /api/v1/saved-issues should return 409 Conflict when issue already saved")
    void saveIssue_DuplicateConflict() throws Exception {
        Long userId = 1L;
        SaveIssueRequestDto request = new SaveIssueRequestDto(
                9999L,
                "facebook/react",
                42,
                "Bug: Hook memoization issue",
                "https://github.com/facebook/react/issues/42"
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(savedIssueService.saveIssue(eq(userId), any(SaveIssueRequestDto.class)))
                .thenThrow(new DuplicateResourceException("Issue 9999 is already saved by user 1"));

        mockMvc.perform(post("/api/v1/saved-issues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(jsonPath("$.message").value("Issue 9999 is already saved by user 1"));
    }

    @Test
    @DisplayName("GET /api/v1/saved-issues should return 200 and list of saved issues")
    void getSavedIssues_Success() throws Exception {
        Long userId = 1L;
        List<SavedIssueResponseDto> list = List.of(
                new SavedIssueResponseDto(100L, userId, 9999L, "facebook/react", 42, "Issue 1", "https://url1", OffsetDateTime.now()),
                new SavedIssueResponseDto(101L, userId, 8888L, "torvalds/linux", 1, "Issue 2", "https://url2", OffsetDateTime.now())
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(savedIssueService.getSavedIssuesByUserId(userId)).thenReturn(list);

        mockMvc.perform(get("/api/v1/saved-issues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[1].id").value(101));

        verify(savedIssueService).getSavedIssuesByUserId(userId);
    }

    @Test
    @DisplayName("GET /api/v1/saved-issues/{id} should return 200 and issue when owned by user")
    void getSavedIssueById_Success() throws Exception {
        Long userId = 1L;
        Long issueId = 100L;
        SavedIssueResponseDto response = new SavedIssueResponseDto(
                issueId, userId, 9999L, "facebook/react", 42, "Issue 1", "https://url1", OffsetDateTime.now()
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(savedIssueService.getSavedIssueByIdAndUserId(issueId, userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/saved-issues/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.repositoryFullName").value("facebook/react"));
    }

    @Test
    @DisplayName("GET /api/v1/saved-issues/{id} should return 404 when not found or not owned")
    void getSavedIssueById_NotFound() throws Exception {
        Long userId = 1L;
        Long issueId = 999L;

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(savedIssueService.getSavedIssueByIdAndUserId(issueId, userId))
                .thenThrow(new ResourceNotFoundException("SavedIssue", "id", issueId));

        mockMvc.perform(get("/api/v1/saved-issues/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("GET /api/v1/saved-issues/check/{githubIssueId} should return check result JSON")
    void checkSavedIssue_True() throws Exception {
        Long userId = 1L;
        Long githubIssueId = 9999L;

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(savedIssueService.isIssueSaved(userId, githubIssueId)).thenReturn(true);

        mockMvc.perform(get("/api/v1/saved-issues/check/9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true));
    }

    @Test
    @DisplayName("DELETE /api/v1/saved-issues/{id} should delete and return 204 No Content")
    void removeSavedIssue_Success() throws Exception {
        Long userId = 1L;
        Long issueId = 100L;

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        doNothing().when(savedIssueService).removeSavedIssue(issueId, userId);

        mockMvc.perform(delete("/api/v1/saved-issues/100"))
                .andExpect(status().isNoContent());

        verify(savedIssueService).removeSavedIssue(issueId, userId);
    }

    @Test
    @DisplayName("DELETE /api/v1/saved-issues/{id} should return 401 when unauthenticated")
    void removeSavedIssue_Unauthenticated() throws Exception {
        when(currentUserProvider.getCurrentUserId())
                .thenThrow(new UnauthenticatedException("Authentication is required to access this resource"));

        mockMvc.perform(delete("/api/v1/saved-issues/100"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
