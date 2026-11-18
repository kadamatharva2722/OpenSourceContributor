package com.osc.backend.user.service;

import com.osc.backend.common.exception.InvalidRequestException;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.user.dto.UpdateUserProfileRequestDto;
import com.osc.backend.user.dto.UserDto;
import com.osc.backend.user.entity.User;
import com.osc.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service managing user identity and profile operations.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Finds a user by internal database ID.
     */
    public UserDto findById(Long id) {
        return UserDto.fromEntity(getUserEntity(id));
    }

    /**
     * Finds a user by their stable GitHub numeric ID.
     */
    public UserDto findByGithubId(Long githubId) {
        if (githubId == null) {
            throw new InvalidRequestException("GitHub ID cannot be null");
        }
        User user = userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "githubId", githubId));
        return UserDto.fromEntity(user);
    }

    /**
     * Checks whether a user with the given internal ID exists.
     */
    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }
        return userRepository.existsById(id);
    }

    /**
     * Checks whether a user with the given GitHub ID exists.
     */
    public boolean existsByGithubId(Long githubId) {
        if (githubId == null) {
            return false;
        }
        return userRepository.existsByGithubId(githubId);
    }

    /**
     * Retrieves an existing user by GitHub ID, or creates a new user if one does not exist.
     * Guarantees that GitHub ID is the permanent identity and avoids duplicate creation.
     */
    @Transactional
    public UserDto getOrCreateUser(Long githubId, String username, String displayName, String avatarUrl, String bio) {
        if (githubId == null) {
            throw new InvalidRequestException("GitHub ID is required");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new InvalidRequestException("Username is required");
        }

        return userRepository.findByGithubId(githubId)
                .map(existingUser -> {
                    // Update latest profile attributes from GitHub on sign-in, while preserving id and githubId
                    existingUser.setUsername(username.trim());
                    if (displayName != null) existingUser.setDisplayName(displayName.trim());
                    if (avatarUrl != null) existingUser.setAvatarUrl(avatarUrl.trim());
                    if (bio != null) existingUser.setBio(bio.trim());
                    User saved = userRepository.save(existingUser);
                    return UserDto.fromEntity(saved);
                })
                .orElseGet(() -> {
                    User newUser = new User(githubId, username.trim(), displayName, avatarUrl, bio);
                    User saved = userRepository.save(newUser);
                    return UserDto.fromEntity(saved);
                });
    }

    /**
     * Updates profile details for an existing user.
     * Note: Does not allow altering the stable external GitHub ID.
     */
    @Transactional
    public UserDto updateProfile(Long userId, UpdateUserProfileRequestDto request) {
        if (request == null) {
            throw new InvalidRequestException("Update profile request cannot be null");
        }

        User user = getUserEntity(userId);

        if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
            user.setUsername(request.getUsername().trim());
        }
        user.setDisplayName(request.getDisplayName() != null ? request.getDisplayName().trim() : null);
        user.setAvatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl().trim() : null);
        user.setBio(request.getBio() != null ? request.getBio().trim() : null);

        User saved = userRepository.save(user);
        return UserDto.fromEntity(saved);
    }

    /**
     * Internal helper to fetch managed User entity for relational services.
     */
    public User getUserEntity(Long id) {
        if (id == null) {
            throw new InvalidRequestException("User ID cannot be null");
        }
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }
}
