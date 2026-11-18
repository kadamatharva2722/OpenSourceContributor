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
class SavedIssueServiceTest {

    @Mock
    private SavedIssueRepository savedIssueRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private SavedIssueService savedIssueService;

    private User sampleUser;
    private SavedIssue sampleSavedIssue;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1001L, "octocat", "The Octocat", null, null);
        sampleUser.setId(1L);

        sampleSavedIssue = new SavedIssue(sampleUser, 99999L, "facebook/react", 101, "Fix state batching bug", "https://github.com/facebook/react/issues/101");
        sampleSavedIssue.setId(50L);
    }

    @Test
    @DisplayName("saveIssue should successfully save issue when not already bookmarked")
    void saveIssue_Success() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(savedIssueRepository.existsByUserIdAndGithubIssueId(1L, 99999L)).thenReturn(false);
        when(savedIssueRepository.save(any(SavedIssue.class))).thenAnswer(invocation -> {
            SavedIssue s = invocation.getArgument(0);
            s.setId(51L);
            return s;
        });

        SaveIssueRequestDto request = new SaveIssueRequestDto(
                99999L, "facebook/react", 101, "Fix state batching bug", "https://github.com/facebook/react/issues/101"
        );

        SavedIssueResponseDto result = savedIssueService.saveIssue(1L, request);

        assertNotNull(result);
        assertEquals(51L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(99999L, result.getGithubIssueId());
        assertEquals("facebook/react", result.getRepositoryFullName());
        verify(savedIssueRepository).save(any(SavedIssue.class));
    }

    @Test
    @DisplayName("saveIssue should throw DuplicateResourceException if user has already saved this GitHub issue")
    void saveIssue_WhenDuplicateIssue_ThrowsDuplicateResourceException() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(savedIssueRepository.existsByUserIdAndGithubIssueId(1L, 99999L)).thenReturn(true);

        SaveIssueRequestDto request = new SaveIssueRequestDto(
                99999L, "facebook/react", 101, "Fix state batching bug", "https://github.com/facebook/react/issues/101"
        );

        assertThrows(DuplicateResourceException.class, () -> savedIssueService.saveIssue(1L, request));
        verify(savedIssueRepository, never()).save(any(SavedIssue.class));
    }

    @Test
    @DisplayName("saveIssue should throw InvalidRequestException when issue fields are invalid")
    void saveIssue_WhenInvalidFields_ThrowsInvalidRequestException() {
        // Negative issue number
        SaveIssueRequestDto req1 = new SaveIssueRequestDto(99999L, "facebook/react", -5, "Title", "https://url");
        assertThrows(InvalidRequestException.class, () -> savedIssueService.saveIssue(1L, req1));

        // Missing title
        SaveIssueRequestDto req2 = new SaveIssueRequestDto(99999L, "facebook/react", 10, "  ", "https://url");
        assertThrows(InvalidRequestException.class, () -> savedIssueService.saveIssue(1L, req2));

        // Missing issue ID
        SaveIssueRequestDto req3 = new SaveIssueRequestDto(null, "facebook/react", 10, "Title", "https://url");
        assertThrows(InvalidRequestException.class, () -> savedIssueService.saveIssue(1L, req3));
    }

    @Test
    @DisplayName("getSavedIssuesByUserId should return all issues belonging to the user")
    void getSavedIssuesByUserId_ReturnsUserIssues() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(savedIssueRepository.findByUserId(1L)).thenReturn(List.of(sampleSavedIssue));

        List<SavedIssueResponseDto> result = savedIssueService.getSavedIssuesByUserId(1L);

        assertEquals(1, result.size());
        assertEquals(99999L, result.get(0).getGithubIssueId());
    }

    @Test
    @DisplayName("getSavedIssueByIdAndUserId should enforce strict user ownership")
    void getSavedIssueByIdAndUserId_EnforcesOwnership() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(savedIssueRepository.findByIdAndUserId(50L, 1L)).thenReturn(Optional.of(sampleSavedIssue));

        SavedIssueResponseDto result = savedIssueService.getSavedIssueByIdAndUserId(50L, 1L);

        assertNotNull(result);
        assertEquals(50L, result.getId());

        // Unauthorized user 2 looking for issue 50
        when(userService.getUserEntity(2L)).thenReturn(new User(2002L, "other", null, null, null));
        when(savedIssueRepository.findByIdAndUserId(50L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> savedIssueService.getSavedIssueByIdAndUserId(50L, 2L));
    }

    @Test
    @DisplayName("removeSavedIssue should delete issue when owned by user")
    void removeSavedIssue_WhenOwned_DeletesSuccessfully() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(savedIssueRepository.findByIdAndUserId(50L, 1L)).thenReturn(Optional.of(sampleSavedIssue));

        savedIssueService.removeSavedIssue(50L, 1L);

        verify(savedIssueRepository).delete(sampleSavedIssue);
    }

    @Test
    @DisplayName("removeSavedIssue should throw ResourceNotFoundException when unauthorized or missing")
    void removeSavedIssue_WhenUnauthorizedOrMissing_ThrowsResourceNotFoundException() {
        when(userService.getUserEntity(2L)).thenReturn(new User(2002L, "other", null, null, null));
        when(savedIssueRepository.findByIdAndUserId(50L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> savedIssueService.removeSavedIssue(50L, 2L));
        verify(savedIssueRepository, never()).delete(any(SavedIssue.class));
    }

    @Test
    @DisplayName("isIssueSaved should return true when issue is saved by user")
    void isIssueSaved_ReturnsCorrectBoolean() {
        when(savedIssueRepository.existsByUserIdAndGithubIssueId(1L, 99999L)).thenReturn(true);
        when(savedIssueRepository.existsByUserIdAndGithubIssueId(1L, 88888L)).thenReturn(false);

        assertTrue(savedIssueService.isIssueSaved(1L, 99999L));
        assertFalse(savedIssueService.isIssueSaved(1L, 88888L));
    }
}
