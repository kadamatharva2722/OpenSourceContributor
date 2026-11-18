package com.osc.backend.account.controller;

import com.osc.backend.account.service.AccountService;
import com.osc.backend.common.exception.GlobalExceptionHandler;
import com.osc.backend.common.exception.ReauthenticationRequiredException;
import com.osc.backend.common.exception.ResourceNotFoundException;
import com.osc.backend.common.exception.UnauthenticatedException;
import com.osc.backend.common.security.CurrentUserProvider;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AccountService accountService;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @InjectMocks
    private AccountController accountController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("DELETE /api/v1/account should return 200 and success message when authenticated")
    void deleteAccount_Success() throws Exception {
        Long authenticatedUserId = 1L;
        when(currentUserProvider.getCurrentUserId()).thenReturn(authenticatedUserId);
        doNothing().when(accountService).deleteAccount(eq(authenticatedUserId), any(), any(), any());

        mockMvc.perform(delete("/api/v1/account")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account and associated data deleted successfully"));

        verify(currentUserProvider).getCurrentUserId();
        verify(accountService).deleteAccount(eq(authenticatedUserId), any(), any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/account should return 401 when user is not authenticated")
    void deleteAccount_Unauthenticated() throws Exception {
        when(currentUserProvider.getCurrentUserId())
                .thenThrow(new UnauthenticatedException("Authentication is required to access this resource"));

        mockMvc.perform(delete("/api/v1/account")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication is required to access this resource"))
                .andExpect(jsonPath("$.path").value("/api/v1/account"));

        verify(accountService, never()).deleteAccount(any(), any(), any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/account should return 403 when fresh re-authentication is required")
    void deleteAccount_ReauthenticationRequired() throws Exception {
        Long authenticatedUserId = 1L;
        when(currentUserProvider.getCurrentUserId()).thenReturn(authenticatedUserId);
        doThrow(new ReauthenticationRequiredException("Recent authentication is required to delete your account. Please sign in again."))
                .when(accountService).deleteAccount(eq(authenticatedUserId), any(), any(), any());

        mockMvc.perform(delete("/api/v1/account")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Recent authentication is required to delete your account. Please sign in again."))
                .andExpect(jsonPath("$.path").value("/api/v1/account"));

        verify(accountService).deleteAccount(eq(authenticatedUserId), any(), any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/account should return 404 when user is not found")
    void deleteAccount_UserNotFound() throws Exception {
        Long authenticatedUserId = 99L;
        when(currentUserProvider.getCurrentUserId()).thenReturn(authenticatedUserId);
        doThrow(new ResourceNotFoundException("User", "id", authenticatedUserId))
                .when(accountService).deleteAccount(eq(authenticatedUserId), any(), any(), any());

        mockMvc.perform(delete("/api/v1/account")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.path").value("/api/v1/account"));

        verify(accountService).deleteAccount(eq(authenticatedUserId), any(), any(), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/account must ignore any user ID supplied in request payload or query parameter")
    void deleteAccount_DerivesUserIdFromSessionNotPayload() throws Exception {
        Long sessionUserId = 5L;
        when(currentUserProvider.getCurrentUserId()).thenReturn(sessionUserId);
        doNothing().when(accountService).deleteAccount(eq(sessionUserId), any(), any(), any());

        // Attempt client user ID spoofing via query params and request body
        mockMvc.perform(delete("/api/v1/account?userId=999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 888, \"id\": 777}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account and associated data deleted successfully"));

        // Verify accountService was called with session user ID 5L, never spoofed IDs
        verify(accountService).deleteAccount(eq(sessionUserId), any(), any(), any());
        verify(accountService, never()).deleteAccount(eq(999L), any(), any(), any());
        verify(accountService, never()).deleteAccount(eq(888L), any(), any(), any());
    }
}
