package com.osc.backend.savedissue.service;

import com.osc.backend.common.exception.DuplicateResourceException;
import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.savedissue.dto.SaveIssueRequestDto;
import com.osc.backend.savedissue.dto.SavedIssueResponseDto;
import com.osc.backend.savedissue.entity.SavedIssue;
import com.osc.backend.savedissue.repository.SavedIssueRepository;
import com.osc.backend.user.entity.User;
import com.osc.backend.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service managing contributor saved/bookmarked issues.
 */
@Service
@Transactional(readOnly = true)
public class SavedIssueService {

    private final SavedIssueRepository savedIssueRepository;
    private final UserService userService;

    public SavedIssueService(SavedIssueRepository savedIssueRepository, UserService userService) {
        this.savedIssueRepository = savedIssueRepository;
        this.userService = userService;
    }

    /**
     * Saves an issue for a user, enforcing ownership and uniqueness constraints.
     */
    @Transactional
    public SavedIssueResponseDto saveIssue(Long userId, SaveIssueRequestDto request) {
        if (request == null) {
            throw new InvalidRequestException("Save issue request cannot be null");
        }
        validateIssueFields(request);

        User user = userService.getUserEntity(userId);

        if (savedIssueRepository.existsByUserIdAndGithubIssueId(userId, request.getGithubIssueId())) {
            throw new DuplicateResourceException(
                    "Issue " + request.getGithubIssueId() + " is already saved by user " + userId
            );
        }

        SavedIssue savedIssue = new SavedIssue(
                user,
                request.getGithubIssueId(),
                request.getRepositoryFullName().trim(),
                request.getIssueNumber(),
                request.getTitle().trim(),
                request.getIssueUrl().trim()
        );

        SavedIssue saved = savedIssueRepository.save(savedIssue);
        return SavedIssueResponseDto.fromEntity(saved);
    }

    /**
     * Retrieves all issues bookmarked by a user.
     */
    public List<SavedIssueResponseDto> getSavedIssuesByUserId(Long userId) {
        userService.getUserEntity(userId); // Verifies user exists
        return savedIssueRepository.findByUserId(userId).stream()
                .map(SavedIssueResponseDto::fromEntity)
                .toList();
    }

    /**
     * Retrieves a single saved issue ensuring strict user ownership.
     */
    public SavedIssueResponseDto getSavedIssueByIdAndUserId(Long id, Long userId) {
        if (id == null) {
            throw new InvalidRequestException("Saved issue ID cannot be null");
        }
        userService.getUserEntity(userId); // Verifies user exists

        SavedIssue savedIssue = savedIssueRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SavedIssue", "id", id));

        return SavedIssueResponseDto.fromEntity(savedIssue);
    }

    /**
     * Checks if a GitHub issue is bookmarked by a user.
     */
    public boolean isIssueSaved(Long userId, Long githubIssueId) {
        if (userId == null || githubIssueId == null) {
            return false;
        }
        return savedIssueRepository.existsByUserIdAndGithubIssueId(userId, githubIssueId);
    }

    /**
     * Removes a saved issue by its internal ID and user ID.
     */
    @Transactional
    public void removeSavedIssue(Long id, Long userId) {
        if (id == null) {
            throw new InvalidRequestException("Saved issue ID cannot be null");
        }
        userService.getUserEntity(userId); // Verifies user exists

        SavedIssue savedIssue = savedIssueRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("SavedIssue", "id", id));

        savedIssueRepository.delete(savedIssue);
    }

    /**
     * Removes a saved issue by GitHub issue ID and user ID.
     */
    @Transactional
    public void removeSavedIssueByGithubIssueId(Long userId, Long githubIssueId) {
        if (githubIssueId == null) {
            throw new InvalidRequestException("GitHub issue ID cannot be null");
        }
        userService.getUserEntity(userId); // Verifies user exists

        SavedIssue savedIssue = savedIssueRepository.findByUserIdAndGithubIssueId(userId, githubIssueId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Saved issue not found for user " + userId + " and GitHub issue " + githubIssueId
                ));

        savedIssueRepository.delete(savedIssue);
    }

    private void validateIssueFields(SaveIssueRequestDto request) {
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
        if (request.getIssueUrl() == null || request.getIssueUrl().trim().isEmpty()) {
            throw new InvalidRequestException("Issue URL is required");
        }
    }
}
