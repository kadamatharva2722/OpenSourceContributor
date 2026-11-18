package com.osc.backend.user.controller;

import com.osc.backend.common.security.CurrentUserProvider;
import com.osc.backend.user.dto.UserDto;
import com.osc.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for retrieving authenticated user details.
 * Prevents user ID tampering by resolving identity strictly from CurrentUserProvider.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User profile operations")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserService userService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     */
    @GetMapping("/me")
    @Operation(summary = "Get current user profile", description = "Retrieves profile information for the authenticated contributor")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - authentication required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserDto> getCurrentUser() {
        Long userId = currentUserProvider.getCurrentUserId();
        UserDto userDto = userService.findById(userId);
        return ResponseEntity.ok(userDto);
    }
}
