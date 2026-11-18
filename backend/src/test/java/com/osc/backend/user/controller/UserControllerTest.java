package com.osc.backend.user.controller;

import com.osc.backend.common.exception.GlobalExceptionHandler;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.common.exception.UnauthenticatedException;
import com.osc.backend.common.security.CurrentUserProvider;
import com.osc.backend.user.dto.UserDto;
import com.osc.backend.user.service.UserService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return 200 and profile for authenticated user")
    void getCurrentUser_Success() throws Exception {
        Long userId = 1L;
        UserDto userDto = new UserDto(
                1L,
                123456L,
                "testuser",
                "Test User",
                "https://avatar.url/test.png",
                "Developer bio",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.findById(userId)).thenReturn(userDto);

        mockMvc.perform(get("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.githubId").value(123456))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.displayName").value("Test User"))
                .andExpect(jsonPath("$.avatarUrl").value("https://avatar.url/test.png"))
                .andExpect(jsonPath("$.bio").value("Developer bio"));

        verify(currentUserProvider).getCurrentUserId();
        verify(userService).findById(userId);
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return 401 when user is unauthenticated")
    void getCurrentUser_Unauthenticated() throws Exception {
        when(currentUserProvider.getCurrentUserId())
                .thenThrow(new UnauthenticatedException("Authentication is required to access this resource"));

        mockMvc.perform(get("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me"))
                .andExpect(jsonPath("$.timestamp").exists());

        verify(userService, never()).findById(any());
    }

    @Test
    @DisplayName("GET /api/v1/users/me should return 404 when user is not found")
    void getCurrentUser_NotFound() throws Exception {
        Long userId = 99L;
        when(currentUserProvider.getCurrentUserId()).thenReturn(userId);
        when(userService.findById(userId))
                .thenThrow(new ResourceNotFoundException("User", "id", userId));

        mockMvc.perform(get("/api/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("User not found with id: '99'"))
                .andExpect(jsonPath("$.path").value("/api/v1/users/me"));
    }
}
