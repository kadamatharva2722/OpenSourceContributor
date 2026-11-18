package com.osc.backend.contribution.service;

import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.contribution.dto.ContributionResponseDto;
import com.osc.backend.contribution.dto.CreateContributionRequestDto;
import com.osc.backend.contribution.dto.UpdateContributionRequestDto;
import com.osc.backend.contribution.entity.Contribution;
import com.osc.backend.contribution.entity.ContributionStatus;
import com.osc.backend.contribution.repository.ContributionRepository;
import com.osc.backend.user.entity.User;
import com.osc.backend.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContributionServiceTest {

    @Mock
    private ContributionRepository contributionRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private ContributionService contributionService;

    private User sampleUser;
    private Contribution sampleContribution;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1001L, "octocat", "The Octocat", null, null);
        sampleUser.setId(1L);

        sampleContribution = new Contribution(
                sampleUser,
                12345L,
                "spring-projects/spring-boot",
                202,
                "Refactor endpoint configuration",
                ContributionStatus.PLANNING,
                "feature/endpoint-refactor",
                null,
                "Initial plan formulated"
        );
        sampleContribution.setId(30L);
    }

    @Test
    @DisplayName("createContribution should create and return new contribution record")
    void createContribution_Success() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(contributionRepository.save(any(Contribution.class))).thenAnswer(invocation -> {
            Contribution c = invocation.getArgument(0);
            c.setId(31L);
            return c;
        });

        CreateContributionRequestDto request = new CreateContributionRequestDto(
                12345L, "spring-projects/spring-boot", 202,
                "Refactor endpoint configuration", ContributionStatus.IN_PROGRESS,
                "feature/branch", null, "Working on diff"
        );

        ContributionResponseDto response = contributionService.createContribution(1L, request);

        assertNotNull(response);
        assertEquals(31L, response.getId());
        assertEquals(1L, response.getUserId());
        assertEquals(ContributionStatus.IN_PROGRESS, response.getStatus());
        assertEquals("feature/branch", response.getBranchName());
        verify(contributionRepository).save(any(Contribution.class));
    }

    @Test
    @DisplayName("createContribution should throw InvalidRequestException when issue number is zero or negative")
    void createContribution_WhenIssueNumberInvalid_ThrowsInvalidRequestException() {
        CreateContributionRequestDto badRequest = new CreateContributionRequestDto(
                12345L, "spring-projects/spring-boot", -1, "Title", null, null, null, null
        );

        assertThrows(InvalidRequestException.class, () -> contributionService.createContribution(1L, badRequest));
        verify(contributionRepository, never()).save(any(Contribution.class));
    }

    @Test
    @DisplayName("getContributionsByUserId should return all contributions for user")
    void getContributionsByUserId_ReturnsUserContributions() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(contributionRepository.findByUserId(1L)).thenReturn(List.of(sampleContribution));

        List<ContributionResponseDto> result = contributionService.getContributionsByUserId(1L);

        assertEquals(1, result.size());
        assertEquals(30L, result.get(0).getId());
    }

    @Test
    @DisplayName("getContributionByIdAndUserId should enforce strict user ownership")
    void getContributionByIdAndUserId_EnforcesOwnership() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(contributionRepository.findByIdAndUserId(30L, 1L)).thenReturn(Optional.of(sampleContribution));

        ContributionResponseDto response = contributionService.getContributionByIdAndUserId(30L, 1L);

        assertNotNull(response);
        assertEquals(30L, response.getId());

        // Unauthorized user 2 attempt
        when(userService.getUserEntity(2L)).thenReturn(new User(2002L, "other", null, null, null));
        when(contributionRepository.findByIdAndUserId(30L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> contributionService.getContributionByIdAndUserId(30L, 2L));
    }

    @Test
    @DisplayName("updateStatus should update contribution lifecycle status")
    void updateStatus_Success() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(contributionRepository.findByIdAndUserId(30L, 1L)).thenReturn(Optional.of(sampleContribution));
        when(contributionRepository.save(any(Contribution.class))).thenReturn(sampleContribution);

        ContributionResponseDto response = contributionService.updateStatus(30L, 1L, ContributionStatus.PR_OPENED);

        assertNotNull(response);
        assertEquals(ContributionStatus.PR_OPENED, sampleContribution.getStatus());
        verify(contributionRepository).save(sampleContribution);
    }

    @Test
    @DisplayName("updateBranchName should update branch name")
    void updateBranchName_Success() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(contributionRepository.findByIdAndUserId(30L, 1L)).thenReturn(Optional.of(sampleContribution));
        when(contributionRepository.save(any(Contribution.class))).thenReturn(sampleContribution);

        contributionService.updateBranchName(30L, 1L, "fix/new-branch-name");

        assertEquals("fix/new-branch-name", sampleContribution.getBranchName());
        verify(contributionRepository).save(sampleContribution);
    }

    @Test
    @DisplayName("updatePullRequestUrl should update PR URL")
    void updatePullRequestUrl_Success() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(contributionRepository.findByIdAndUserId(30L, 1L)).thenReturn(Optional.of(sampleContribution));
        when(contributionRepository.save(any(Contribution.class))).thenReturn(sampleContribution);

        contributionService.updatePullRequestUrl(30L, 1L, "https://github.com/spring-projects/spring-boot/pull/999");

        assertEquals("https://github.com/spring-projects/spring-boot/pull/999", sampleContribution.getPullRequestUrl());
        verify(contributionRepository).save(sampleContribution);
    }

    @Test
    @DisplayName("updateContributionDetails should update multiple fields at once")
    void updateContributionDetails_Success() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(contributionRepository.findByIdAndUserId(30L, 1L)).thenReturn(Optional.of(sampleContribution));
        when(contributionRepository.save(any(Contribution.class))).thenReturn(sampleContribution);

        UpdateContributionRequestDto request = new UpdateContributionRequestDto(
                ContributionStatus.COMPLETED,
                "main",
                "https://github.com/spring-projects/spring-boot/pull/999",
                "Contribution completed by contributor"
        );

        ContributionResponseDto response = contributionService.updateContributionDetails(30L, 1L, request);

        assertNotNull(response);
        assertEquals(ContributionStatus.COMPLETED, sampleContribution.getStatus());
        assertEquals("main", sampleContribution.getBranchName());
        assertEquals("Contribution completed by contributor", sampleContribution.getNotes());
    }

    @Test
    @DisplayName("filterByStatus should return only contributions with matching status")
    void filterByStatus_Success() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(contributionRepository.findByUserIdAndStatus(1L, ContributionStatus.PLANNING)).thenReturn(List.of(sampleContribution));

        List<ContributionResponseDto> result = contributionService.getContributionsByUserIdAndStatus(1L, ContributionStatus.PLANNING);

        assertEquals(1, result.size());
        assertEquals(ContributionStatus.PLANNING, result.get(0).getStatus());
    }

    @Test
    @DisplayName("deleteContribution should delete contribution when owned by user")
    void deleteContribution_WhenOwned_DeletesSuccessfully() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(contributionRepository.findByIdAndUserId(30L, 1L)).thenReturn(Optional.of(sampleContribution));

        contributionService.deleteContribution(30L, 1L);

        verify(contributionRepository).delete(sampleContribution);
    }

    @Test
    @DisplayName("deleteContribution should throw ResourceNotFoundException when unauthorized")
    void deleteContribution_WhenUnauthorized_ThrowsResourceNotFoundException() {
        when(userService.getUserEntity(2L)).thenReturn(new User(2002L, "other", null, null, null));
        when(contributionRepository.findByIdAndUserId(30L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> contributionService.deleteContribution(30L, 2L));
        verify(contributionRepository, never()).delete(any(Contribution.class));
    }
}
