package com.osc.backend.userpreference.service;

import com.osc.backend.common.exception.DuplicateResourceException;
import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.user.entity.User;
import com.osc.backend.user.service.UserService;
import com.osc.backend.userpreference.dto.UpdateUserPreferenceRequestDto;
import com.osc.backend.userpreference.dto.UserPreferenceResponseDto;
import com.osc.backend.userpreference.entity.UserPreference;
import com.osc.backend.userpreference.repository.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserPreferenceServiceTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserPreferenceService userPreferenceService;

    private User sampleUser;
    private UserPreference samplePreference;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1001L, "octocat", "The Octocat", null, null);
        sampleUser.setId(1L);

        samplePreference = new UserPreference(sampleUser, List.of("Java", "TypeScript"), List.of("Web", "DevOps"), "INTERMEDIATE");
        samplePreference.setId(10L);
    }

    @Test
    @DisplayName("getPreferencesByUserId should return response DTO when preferences exist")
    void getPreferencesByUserId_WhenFound_ReturnsDto() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(samplePreference));

        UserPreferenceResponseDto result = userPreferenceService.getPreferencesByUserId(1L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(1L, result.getUserId());
        assertEquals(List.of("Java", "TypeScript"), result.getPreferredLanguages());
        assertEquals("INTERMEDIATE", result.getDifficulty());
    }

    @Test
    @DisplayName("getPreferencesByUserId should throw ResourceNotFoundException when preferences do not exist")
    void getPreferencesByUserId_WhenNotFound_ThrowsResourceNotFoundException() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userPreferenceService.getPreferencesByUserId(1L));
    }

    @Test
    @DisplayName("createPreferences should create and save new preferences")
    void createPreferences_Success() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(userPreferenceRepository.existsByUserId(1L)).thenReturn(false);
        when(userPreferenceRepository.save(any(UserPreference.class))).thenAnswer(invocation -> {
            UserPreference p = invocation.getArgument(0);
            p.setId(11L);
            return p;
        });

        UpdateUserPreferenceRequestDto request = new UpdateUserPreferenceRequestDto(
                List.of("Python", "Rust"),
                List.of("CLI"),
                "BEGINNER"
        );

        UserPreferenceResponseDto response = userPreferenceService.createPreferences(1L, request);

        assertNotNull(response);
        assertEquals(11L, response.getId());
        assertEquals(List.of("Python", "Rust"), response.getPreferredLanguages());
        assertEquals("BEGINNER", response.getDifficulty());
        verify(userPreferenceRepository).save(any(UserPreference.class));
    }

    @Test
    @DisplayName("createPreferences should throw DuplicateResourceException if user already has preferences")
    void createPreferences_WhenAlreadyExists_ThrowsDuplicateResourceException() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(userPreferenceRepository.existsByUserId(1L)).thenReturn(true);

        UpdateUserPreferenceRequestDto request = new UpdateUserPreferenceRequestDto(
                List.of("Java"),
                List.of("Web"),
                "BEGINNER"
        );

        assertThrows(DuplicateResourceException.class, () -> userPreferenceService.createPreferences(1L, request));
        verify(userPreferenceRepository, never()).save(any(UserPreference.class));
    }

    @Test
    @DisplayName("createPreferences should throw InvalidRequestException for unsupported difficulty values")
    void createPreferences_WhenDifficultyInvalid_ThrowsInvalidRequestException() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(userPreferenceRepository.existsByUserId(1L)).thenReturn(false);

        UpdateUserPreferenceRequestDto request = new UpdateUserPreferenceRequestDto(
                List.of("Java"),
                List.of("Web"),
                "SUPER_HARD_CATEGORY" // invalid difficulty
        );

        assertThrows(InvalidRequestException.class, () -> userPreferenceService.createPreferences(1L, request));
    }

    @Test
    @DisplayName("updatePreferences should update existing preference fields correctly")
    void updatePreferences_Success() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(samplePreference));
        when(userPreferenceRepository.save(any(UserPreference.class))).thenReturn(samplePreference);

        UpdateUserPreferenceRequestDto request = new UpdateUserPreferenceRequestDto(
                List.of("Kotlin", "Go"),
                List.of("Cloud"),
                "ADVANCED"
        );

        UserPreferenceResponseDto updated = userPreferenceService.updatePreferences(1L, request);

        assertNotNull(updated);
        assertEquals(List.of("Kotlin", "Go"), samplePreference.getPreferredLanguages());
        assertEquals(List.of("Cloud"), samplePreference.getPreferredDomains());
        assertEquals("ADVANCED", samplePreference.getDifficulty());
    }

    @Test
    @DisplayName("getOrCreateDefaultPreferences should return existing preferences if already present")
    void getOrCreateDefaultPreferences_WhenAlreadyExists_ReturnsExisting() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.of(samplePreference));

        UserPreferenceResponseDto response = userPreferenceService.getOrCreateDefaultPreferences(1L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        verify(userPreferenceRepository, never()).save(any(UserPreference.class));
    }

    @Test
    @DisplayName("getOrCreateDefaultPreferences should create default empty preferences when missing")
    void getOrCreateDefaultPreferences_WhenMissing_CreatesDefault() {
        when(userService.getUserEntity(1L)).thenReturn(sampleUser);
        when(userPreferenceRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(userPreferenceRepository.save(any(UserPreference.class))).thenAnswer(invocation -> {
            UserPreference p = invocation.getArgument(0);
            p.setId(12L);
            return p;
        });

        UserPreferenceResponseDto response = userPreferenceService.getOrCreateDefaultPreferences(1L);

        assertNotNull(response);
        assertEquals(12L, response.getId());
        assertEquals(new ArrayList<>(), response.getPreferredLanguages());
        assertEquals(new ArrayList<>(), response.getPreferredDomains());
        assertNull(response.getDifficulty());
        verify(userPreferenceRepository).save(any(UserPreference.class));
    }
}
