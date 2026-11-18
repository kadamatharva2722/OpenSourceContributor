package com.osc.backend.user.service;

import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.user.dto.UpdateUserProfileRequestDto;
import com.osc.backend.user.dto.UserDto;
import com.osc.backend.user.entity.User;
import com.osc.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = new User(1001L, "octocat", "The Octocat", "https://github.com/avatar.png", "Bio");
        sampleUser.setId(1L);
    }

    @Test
    @DisplayName("findById should return UserDto when user exists")
    void findById_WhenUserExists_ReturnsUserDto() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserDto result = userService.findById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(1001L, result.getGithubId());
        assertEquals("octocat", result.getUsername());
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("findById should throw ResourceNotFoundException when user is missing")
    void findById_WhenUserDoesNotExist_ThrowsResourceNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findById(99L));
    }

    @Test
    @DisplayName("findByGithubId should return UserDto when user exists")
    void findByGithubId_WhenUserExists_ReturnsUserDto() {
        when(userRepository.findByGithubId(1001L)).thenReturn(Optional.of(sampleUser));

        UserDto result = userService.findByGithubId(1001L);

        assertNotNull(result);
        assertEquals(1001L, result.getGithubId());
        verify(userRepository).findByGithubId(1001L);
    }

    @Test
    @DisplayName("findByGithubId should throw ResourceNotFoundException when GitHub identity is missing")
    void findByGithubId_WhenUserMissing_ThrowsResourceNotFoundException() {
        when(userRepository.findByGithubId(9999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.findByGithubId(9999L));
    }

    @Test
    @DisplayName("getOrCreateUser should create a new user when GitHub identity is not registered")
    void getOrCreateUser_WhenUserDoesNotExist_CreatesUser() {
        when(userRepository.findByGithubId(2002L)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(2L);
            return u;
        });

        UserDto created = userService.getOrCreateUser(2002L, "newuser", "New User", "https://avatar.png", "New Bio");

        assertNotNull(created);
        assertEquals(2L, created.getId());
        assertEquals(2002L, created.getGithubId());
        assertEquals("newuser", created.getUsername());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("getOrCreateUser should update profile info and preserve existing identity when user exists")
    void getOrCreateUser_WhenUserAlreadyExists_UpdatesProfileAndPreservesIdentity() {
        when(userRepository.findByGithubId(1001L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserDto existing = userService.getOrCreateUser(1001L, "octocat-updated", "Updated Name", "https://new-avatar.png", "Updated bio");

        assertNotNull(existing);
        assertEquals(1001L, existing.getGithubId());
        assertEquals("octocat-updated", sampleUser.getUsername());
        assertEquals("Updated Name", sampleUser.getDisplayName());
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("getOrCreateUser should throw InvalidRequestException when required fields are missing")
    void getOrCreateUser_WhenRequiredFieldsMissing_ThrowsInvalidRequestException() {
        assertThrows(InvalidRequestException.class, () ->
                userService.getOrCreateUser(null, "username", "Name", null, null));
        assertThrows(InvalidRequestException.class, () ->
                userService.getOrCreateUser(1001L, "", "Name", null, null));
    }

    @Test
    @DisplayName("updateProfile should update user attributes and preserve githubId")
    void updateProfile_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UpdateUserProfileRequestDto request = new UpdateUserProfileRequestDto("renamed", "New Display", "https://avatar.png", "New Bio");
        UserDto updated = userService.updateProfile(1L, request);

        assertNotNull(updated);
        assertEquals("renamed", sampleUser.getUsername());
        assertEquals("New Display", sampleUser.getDisplayName());
        assertEquals(1001L, sampleUser.getGithubId()); // GitHub ID unchanged
        verify(userRepository).save(sampleUser);
    }

    @Test
    @DisplayName("existsByGithubId should return correct boolean")
    void existsByGithubId_ReturnsCorrectBoolean() {
        when(userRepository.existsByGithubId(1001L)).thenReturn(true);
        when(userRepository.existsByGithubId(9999L)).thenReturn(false);

        assertTrue(userService.existsByGithubId(1001L));
        assertFalse(userService.existsByGithubId(9999L));
        assertFalse(userService.existsByGithubId(null));
    }
}
