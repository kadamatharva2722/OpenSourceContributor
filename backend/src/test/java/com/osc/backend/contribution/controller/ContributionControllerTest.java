package com.osc.backend.contribution.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.common.exception.GlobalExceptionHandler;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.common.exception.UnauthenticatedException;
import com.osc.backend.common.security.CurrentUserProvider;
import com.osc.backend.contribution.dto.ContributionResponseDto;
import com.osc.backend.contribution.dto.CreateContributionRequestDto;
import com.osc.backend.contribution.dto.UpdateContributionRequestDto;
import com.osc.backend.contribution.entity.ContributionStatus;
import com.osc.backend.contribution.service.ContributionService;
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
class ContributionControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private ContributionService contributionService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private ContributionController contributionController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(contributionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/contributions should create contribution and return 201 Created")
    void createContribution_Success() throws Exception {
        Long userId = 1L;
        CreateContributionRequestDto request = new CreateContributionRequestDto(
                5555L,
                "torvalds/linux",
                101,
                "Fix memory leak in network driver",
                ContributionStatus.IN_PROGRESS,
                "fix/net-driver",
                "https://github.com/torvalds/linux/pull/101",
                "Working on unit tests"
        );

        ContributionResponseDto response = new ContributionResponseDto(
                50L,
                userId,
                5555L,
                "torvalds/linux",
                101,
                "Fix memory leak in network driver",
                ContributionStatus.IN_PROGRESS,
                "fix/net-driver",
                "https://github.com/torvalds/linux/pull/101",
                "Working on unit tests",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(contributionService.createContribution(eq(userId), any(CreateContributionRequestDto.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.branchName").value("fix/net-driver"))
                .andExpect(jsonPath("$.repositoryFullName").value("torvalds/linux"));

        verify(contributionService).createContribution(eq(userId), any(CreateContributionRequestDto.class));
    }

    @Test
    @DisplayName("POST /api/v1/contributions should return 400 Bad Request on invalid payload")
    void createContribution_ValidationFailure() throws Exception {
        CreateContributionRequestDto request = new CreateContributionRequestDto();
        // Missing required fields

        mockMvc.perform(post("/api/v1/contributions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("GET /api/v1/contributions without status should list all user contributions")
    void getContributions_All() throws Exception {
        Long userId = 1L;
        List<ContributionResponseDto> list = List.of(
                new ContributionResponseDto(50L, userId, 5555L, "torvalds/linux", 101, "Fix 1", ContributionStatus.PLANNING, null, null, null, OffsetDateTime.now(), OffsetDateTime.now()),
                new ContributionResponseDto(51L, userId, 6666L, "facebook/react", 202, "Fix 2", ContributionStatus.COMPLETED, null, null, null, OffsetDateTime.now(), OffsetDateTime.now())
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(contributionService.getContributionsByUserId(userId)).thenReturn(list);

        mockMvc.perform(get("/api/v1/contributions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(50))
                .andExpect(jsonPath("$[1].id").value(51));

        verify(contributionService).getContributionsByUserId(userId);
        verify(contributionService, never()).getContributionsByUserIdAndStatus(any(), any());
    }

    @Test
    @DisplayName("GET /api/v1/contributions?status=IN_PROGRESS should filter by status")
    void getContributions_FilteredByStatus() throws Exception {
        Long userId = 1L;
        List<ContributionResponseDto> list = List.of(
                new ContributionResponseDto(50L, userId, 5555L, "torvalds/linux", 101, "Fix 1", ContributionStatus.IN_PROGRESS, null, null, null, OffsetDateTime.now(), OffsetDateTime.now())
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(contributionService.getContributionsByUserIdAndStatus(userId, ContributionStatus.IN_PROGRESS)).thenReturn(list);

        mockMvc.perform(get("/api/v1/contributions").param("status", "IN_PROGRESS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("IN_PROGRESS"));

        verify(contributionService).getContributionsByUserIdAndStatus(userId, ContributionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("GET /api/v1/contributions?status=INVALID_STATUS should return 400 Bad Request")
    void getContributions_InvalidEnumStatus() throws Exception {
        mockMvc.perform(get("/api/v1/contributions").param("status", "NOT_A_VALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Invalid value 'NOT_A_VALID_STATUS' for parameter 'status'"));
    }

    @Test
    @DisplayName("GET /api/v1/contributions/{id} should return contribution when owned")
    void getContributionById_Success() throws Exception {
        Long userId = 1L;
        Long id = 50L;
        ContributionResponseDto response = new ContributionResponseDto(
                id, userId, 5555L, "torvalds/linux", 101, "Fix 1", ContributionStatus.PLANNING, null, null, null, OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(contributionService.getContributionByIdAndUserId(id, userId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/contributions/50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(50));
    }

    @Test
    @DisplayName("GET /api/v1/contributions/{id} should return 404 when not found")
    void getContributionById_NotFound() throws Exception {
        Long userId = 1L;
        Long id = 999L;

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(contributionService.getContributionByIdAndUserId(id, userId))
                .thenThrow(new ResourceNotFoundException("Contribution", "id", id));

        mockMvc.perform(get("/api/v1/contributions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("PATCH /api/v1/contributions/{id} should update contribution and return 200")
    void updateContribution_Success() throws Exception {
        Long userId = 1L;
        Long id = 50L;
        UpdateContributionRequestDto request = new UpdateContributionRequestDto(
                ContributionStatus.COMPLETED,
                "feature/done",
                "https://github.com/torvalds/linux/pull/101",
                "Merged by maintainer"
        );

        ContributionResponseDto response = new ContributionResponseDto(
                id, userId, 5555L, "torvalds/linux", 101, "Fix 1",
                ContributionStatus.COMPLETED, "feature/done",
                "https://github.com/torvalds/linux/pull/101", "Merged by maintainer",
                OffsetDateTime.now(), OffsetDateTime.now()
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(contributionService.updateContributionDetails(eq(id), eq(userId), any(UpdateContributionRequestDto.class)))
                .thenReturn(response);

        mockMvc.perform(patch("/api/v1/contributions/50")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.branchName").value("feature/done"))
                .andExpect(jsonPath("$.notes").value("Merged by maintainer"));

        verify(contributionService).updateContributionDetails(eq(id), eq(userId), any(UpdateContributionRequestDto.class));
    }

    @Test
    @DisplayName("DELETE /api/v1/contributions/{id} should delete contribution and return 204")
    void deleteContribution_Success() throws Exception {
        Long userId = 1L;
        Long id = 50L;

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        doNothing().when(contributionService).deleteContribution(id, userId);

        mockMvc.perform(delete("/api/v1/contributions/50"))
                .andExpect(status().isNoContent());

        verify(contributionService).deleteContribution(id, userId);
    }
}
