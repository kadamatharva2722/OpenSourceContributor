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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Service managing contributor discovery preferences.
 */
@Service
@Transactional(readOnly = true)
public class UserPreferenceService {

    private static final Set<String> ALLOWED_DIFFICULTIES = Set.of("BEGINNER", "INTERMEDIATE", "ADVANCED");

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserService userService;

    public UserPreferenceService(UserPreferenceRepository userPreferenceRepository, UserService userService) {
        this.userPreferenceRepository = userPreferenceRepository;
        this.userService = userService;
    }

    /**
     * Retrieves preferences for a given user.
     */
    public UserPreferenceResponseDto getPreferencesByUserId(Long userId) {
        userService.getUserEntity(userId); // Verifies user exists
        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserPreference", "userId", userId));
        return UserPreferenceResponseDto.fromEntity(preference);
    }

    /**
     * Creates an initial preference record for a user.
     * Enforces that each user has at most one preference record.
     */
    @Transactional
    public UserPreferenceResponseDto createPreferences(Long userId, UpdateUserPreferenceRequestDto request) {
        User user = userService.getUserEntity(userId);

        if (userPreferenceRepository.existsByUserId(userId)) {
            throw new DuplicateResourceException("UserPreference", "userId", userId);
        }

        String validatedDifficulty = validateDifficulty(request != null ? request.getDifficulty() : null);
        List<String> languages = sanitizeList(request != null ? request.getPreferredLanguages() : null);
        List<String> domains = sanitizeList(request != null ? request.getPreferredDomains() : null);

        UserPreference preference = new UserPreference(user, languages, domains, validatedDifficulty);
        UserPreference saved = userPreferenceRepository.save(preference);
        return UserPreferenceResponseDto.fromEntity(saved);
    }

    /**
     * Updates an existing user's preferences.
     */
    @Transactional
    public UserPreferenceResponseDto updatePreferences(Long userId, UpdateUserPreferenceRequestDto request) {
        userService.getUserEntity(userId); // Verifies user exists

        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("UserPreference", "userId", userId));

        if (request != null) {
            if (request.getDifficulty() != null) {
                preference.setDifficulty(validateDifficulty(request.getDifficulty()));
            }
            if (request.getPreferredLanguages() != null) {
                preference.setPreferredLanguages(sanitizeList(request.getPreferredLanguages()));
            }
            if (request.getPreferredDomains() != null) {
                preference.setPreferredDomains(sanitizeList(request.getPreferredDomains()));
            }
        }

        UserPreference saved = userPreferenceRepository.save(preference);
        return UserPreferenceResponseDto.fromEntity(saved);
    }

    /**
     * Creates or updates preferences for a user in an idempotent manner.
     */
    @Transactional
    public UserPreferenceResponseDto createOrUpdatePreferences(Long userId, UpdateUserPreferenceRequestDto request) {
        userService.getUserEntity(userId);
        if (userPreferenceRepository.existsByUserId(userId)) {
            return updatePreferences(userId, request);
        } else {
            return createPreferences(userId, request);
        }
    }

    /**
     * Retrieves preferences for a user, or initializes default empty preferences if none exist.
     */
    @Transactional
    public UserPreferenceResponseDto getOrCreateDefaultPreferences(Long userId) {
        User user = userService.getUserEntity(userId);

        return userPreferenceRepository.findByUserId(userId)
                .map(UserPreferenceResponseDto::fromEntity)
                .orElseGet(() -> {
                    UserPreference defaultPreference = new UserPreference(user, new ArrayList<>(), new ArrayList<>(), null);
                    UserPreference saved = userPreferenceRepository.save(defaultPreference);
                    return UserPreferenceResponseDto.fromEntity(saved);
                });
    }

    private String validateDifficulty(String difficulty) {
        if (difficulty == null || difficulty.trim().isEmpty()) {
            return null;
        }
        String normalized = difficulty.trim().toUpperCase();
        if (!ALLOWED_DIFFICULTIES.contains(normalized)) {
            throw new InvalidRequestException(
                    "Invalid difficulty '" + difficulty + "'. Allowed values: " + ALLOWED_DIFFICULTIES
            );
        }
        return normalized;
    }

    private List<String> sanitizeList(List<String> input) {
        if (input == null) {
            return new ArrayList<>();
        }
        return input.stream()
                .filter(item -> item != null && !item.trim().isEmpty())
                .map(String::trim)
                .toList();
    }
}
