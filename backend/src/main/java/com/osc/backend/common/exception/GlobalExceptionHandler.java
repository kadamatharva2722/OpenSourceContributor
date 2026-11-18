package com.osc.backend.common.exception;

import com.osc.backend.common.dto.ErrorResponseDto;
import com.osc.backend.discovery.exception.AiQueryParsingException;
import com.osc.backend.discovery.exception.AiServiceUnavailableException;
import com.osc.backend.discovery.exception.DiscoveryException;
import com.osc.backend.github.exception.GitHubApiException;
import com.osc.backend.github.exception.GitHubApiUnavailableException;
import com.osc.backend.github.exception.GitHubInvalidResponseException;
import com.osc.backend.github.exception.GitHubRateLimitExceededException;
import com.osc.backend.github.exception.GitHubResourceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.OffsetDateTime;
import java.util.stream.Collectors;

/**
 * Centralized exception handler for REST controllers.
 * Maps domain and framework exceptions to consistent JSON error responses.
 * Ensures internal database/system details and stack traces are not leaked.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {} (path: {})", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleNoResourceFoundException(
            org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Resource path not found: {} ({})", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, "Resource not found: " + request.getRequestURI(), request.getRequestURI());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateResourceException(
            DuplicateResourceException ex, HttpServletRequest request) {
        log.warn("Duplicate resource conflict: {} (path: {})", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidRequestException(
            InvalidRequestException ex, HttpServletRequest request) {
        log.warn("Invalid request: {} (path: {})", ex.getMessage(), request.getRequestURI());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        String validationErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " +
                        (fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : "invalid"))
                .sorted()
                .collect(Collectors.joining("; "));

        if (validationErrors.isEmpty()) {
            validationErrors = "Request validation failed";
        }

        log.warn("Validation failed for {}: {}", request.getRequestURI(), validationErrors);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, validationErrors, request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDto> handleTypeMismatchException(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = String.format("Invalid value '%s' for parameter '%s'", ex.getValue(), ex.getName());
        log.warn("Type mismatch on {}: {}", request.getRequestURI(), message);
        return buildErrorResponse(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDto> handleMalformedJsonException(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed JSON request body on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Malformed or unreadable JSON request body", request.getRequestURI());
    }

    @ExceptionHandler(AiQuotaExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleAiQuotaExceededException(
            AiQuotaExceededException ex, HttpServletRequest request) {
        log.warn("AI quota exceeded for {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler({UnauthenticatedException.class, AuthenticationException.class})
    public ResponseEntity<ErrorResponseDto> handleAuthenticationException(
            Exception ex, HttpServletRequest request) {
        log.warn("Unauthenticated access to {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(ReauthenticationRequiredException.class)
    public ResponseEntity<ErrorResponseDto> handleReauthenticationRequiredException(
            ReauthenticationRequiredException ex, HttpServletRequest request) {
        log.warn("Re-authentication required for {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied for {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "Access is denied", request.getRequestURI());
    }

    @ExceptionHandler(ClientRateLimitExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleClientRateLimitExceededException(
            ClientRateLimitExceededException ex, HttpServletRequest request) {
        log.warn("Client rate limit exceeded for {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(org.springframework.http.HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()))
                .body(new ErrorResponseDto(
                        OffsetDateTime.now(),
                        HttpStatus.TOO_MANY_REQUESTS.value(),
                        HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                        ex.getMessage(),
                        request.getRequestURI()
                ));
    }

    @ExceptionHandler(GitHubRateLimitExceededException.class)
    public ResponseEntity<ErrorResponseDto> handleGitHubRateLimitExceededException(
            GitHubRateLimitExceededException ex, HttpServletRequest request) {
        log.warn("GitHub rate limit exceeded for {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(GitHubResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleGitHubResourceNotFoundException(
            GitHubResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("GitHub resource not found for {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(GitHubApiUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleGitHubApiUnavailableException(
            GitHubApiUnavailableException ex, HttpServletRequest request) {
        log.error("GitHub API unavailable for {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, "GitHub service is temporarily unavailable. Please try again later.", request.getRequestURI());
    }

    @ExceptionHandler(GitHubInvalidResponseException.class)
    public ResponseEntity<ErrorResponseDto> handleGitHubInvalidResponseException(
            GitHubInvalidResponseException ex, HttpServletRequest request) {
        log.error("Invalid GitHub API response for {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, "Unexpected response received from GitHub API.", request.getRequestURI());
    }

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<ErrorResponseDto> handleGitHubApiException(
            GitHubApiException ex, HttpServletRequest request) {
        log.error("GitHub API error on {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_GATEWAY, "GitHub API communication error: " + ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ErrorResponseDto> handleAiServiceUnavailableException(
            AiServiceUnavailableException ex, HttpServletRequest request) {
        log.warn("AI discovery service unavailable for {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(AiQueryParsingException.class)
    public ResponseEntity<ErrorResponseDto> handleAiQueryParsingException(
            AiQueryParsingException ex, HttpServletRequest request) {
        log.warn("AI query parsing failed for {}: {}", request.getRequestURI(), ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(DiscoveryException.class)
    public ResponseEntity<ErrorResponseDto> handleDiscoveryException(
            DiscoveryException ex, HttpServletRequest request) {
        log.error("AI discovery error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An error occurred during AI discovery.", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal server error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.",
                request.getRequestURI()
        );
    }

    private ResponseEntity<ErrorResponseDto> buildErrorResponse(HttpStatus status, String message, String path) {
        ErrorResponseDto errorDto = new ErrorResponseDto(
                OffsetDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        return ResponseEntity.status(status).body(errorDto);
    }
}
