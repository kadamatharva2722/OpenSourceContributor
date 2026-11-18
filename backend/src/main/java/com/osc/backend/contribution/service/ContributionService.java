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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing user open-source contribution tracking records.
 */
@Service
@Transactional(readOnly = true)
public class ContributionService {

    private final ContributionRepository contributionRepository;
    private final UserService userService;

    public ContributionService(ContributionRepository contributionRepository, UserService userService) {
        this.contributionRepository = contributionRepository;
        this.userService = userService;
    }

    /**
     * Creates a new contribution record for a user.
     * Note: Multiple contribution records for the same issue are permitted (e.g. distinct attempts or branches).
     */
    @Transactional
    public ContributionResponseDto createContribution(Long userId, CreateContributionRequestDto request) {
        if (request == null) {
            throw new InvalidRequestException("Create contribution request cannot be null");
        }
        validateContributionFields(request);

        User user = userService.getUserEntity(userId);

        ContributionStatus initialStatus = request.getStatus() != null ? request.getStatus() : ContributionStatus.PLANNING;

        Contribution contribution = new Contribution(
                user,
                request.getGithubIssueId(),
                request.getRepositoryFullName().trim(),
                request.getIssueNumber(),
                request.getTitle().trim(),
                initialStatus,
                request.getBranchName() != null ? request.getBranchName().trim() : null,
                request.getPullRequestUrl() != null ? request.getPullRequestUrl().trim() : null,
                request.getNotes() != null ? request.getNotes().trim() : null
        );

        Contribution saved = contributionRepository.save(contribution);
        return ContributionResponseDto.fromEntity(saved);
    }

    /**
     * Retrieves all contributions tracked by a user.
     */
    public List<ContributionResponseDto> getContributionsByUserId(Long userId) {
        userService.getUserEntity(userId); // Verifies user exists
        return contributionRepository.findByUserId(userId).stream()
                .map(ContributionResponseDto::fromEntity)
                .toList();
    }

    /**
     * Retrieves a specific contribution by ID, enforcing user ownership.
     */
    public ContributionResponseDto getContributionByIdAndUserId(Long id, Long userId) {
        return ContributionResponseDto.fromEntity(getContributionEntity(id, userId));
    }

    /**
     * Filters a user's contributions by lifecycle status.
     */
    public List<ContributionResponseDto> getContributionsByUserIdAndStatus(Long userId, ContributionStatus status) {
        if (status == null) {
            throw new InvalidRequestException("Contribution status cannot be null");
        }
        userService.getUserEntity(userId); // Verifies user exists
        return contributionRepository.findByUserIdAndStatus(userId, status).stream()
                .map(ContributionResponseDto::fromEntity)
                .toList();
    }

    /**
     * Updates the status of a contribution.
     * Note: COMPLETED represents a status reported by the user and does not imply PR merge verification by GitHub.
     */
    @Transactional
    public ContributionResponseDto updateStatus(Long id, Long userId, ContributionStatus newStatus) {
        if (newStatus == null) {
            throw new InvalidRequestException("New contribution status cannot be null");
        }
        Contribution contribution = getContributionEntity(id, userId);
        contribution.setStatus(newStatus);
        Contribution saved = contributionRepository.save(contribution);
        return ContributionResponseDto.fromEntity(saved);
    }

    /**
     * Updates the active Git branch name for a contribution.
     */
    @Transactional
    public ContributionResponseDto updateBranchName(Long id, Long userId, String branchName) {
        Contribution contribution = getContributionEntity(id, userId);
        contribution.setBranchName(branchName != null ? branchName.trim() : null);
        Contribution saved = contributionRepository.save(contribution);
        return ContributionResponseDto.fromEntity(saved);
    }

    /**
     * Updates the pull request URL for a contribution.
     */
    @Transactional
    public ContributionResponseDto updatePullRequestUrl(Long id, Long userId, String pullRequestUrl) {
        Contribution contribution = getContributionEntity(id, userId);
        contribution.setPullRequestUrl(pullRequestUrl != null ? pullRequestUrl.trim() : null);
        Contribution saved = contributionRepository.save(contribution);
        return ContributionResponseDto.fromEntity(saved);
    }

    /**
     * Updates contributor notes.
     */
    @Transactional
    public ContributionResponseDto updateNotes(Long id, Long userId, String notes) {
        Contribution contribution = getContributionEntity(id, userId);
        contribution.setNotes(notes != null ? notes.trim() : null);
        Contribution saved = contributionRepository.save(contribution);
        return ContributionResponseDto.fromEntity(saved);
    }

    /**
     * Updates contribution fields in a single operation.
     */
    @Transactional
    public ContributionResponseDto updateContributionDetails(Long id, Long userId, UpdateContributionRequestDto request) {
        if (request == null) {
            throw new InvalidRequestException("Update contribution request cannot be null");
        }
        Contribution contribution = getContributionEntity(id, userId);

        if (request.getStatus() != null) {
            contribution.setStatus(request.getStatus());
        }
        if (request.getBranchName() != null) {
            contribution.setBranchName(request.getBranchName().trim());
        }
        if (request.getPullRequestUrl() != null) {
            contribution.setPullRequestUrl(request.getPullRequestUrl().trim());
        }
        if (request.getNotes() != null) {
            contribution.setNotes(request.getNotes().trim());
        }

        Contribution saved = contributionRepository.save(contribution);
        return ContributionResponseDto.fromEntity(saved);
    }

    /**
     * Deletes a contribution record, enforcing user ownership.
     */
    @Transactional
    public void deleteContribution(Long id, Long userId) {
        Contribution contribution = getContributionEntity(id, userId);
        contributionRepository.delete(contribution);
    }

    private Contribution getContributionEntity(Long id, Long userId) {
        if (id == null) {
            throw new InvalidRequestException("Contribution ID cannot be null");
        }
        userService.getUserEntity(userId); // Verifies user exists

        return contributionRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Contribution", "id", id));
    }

    private void validateContributionFields(CreateContributionRequestDto request) {
        if (request.getGithubIssueId() == null) {
            throw new InvalidRequestException("GitHub issue ID is required");
        }
        if (request.getRepositoryFullName() == null || request.getRepositoryFullName().trim().isEmpty()) {
            throw new InvalidRequestException("Repository full name is required");
        }
        if (request.getIssueNumber() == null || request.getIssueNumber() <= 0) {
            throw new InvalidRequestException("Issue number must be a positive integer");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new InvalidRequestException("Issue title is required");
        }
    }
}
