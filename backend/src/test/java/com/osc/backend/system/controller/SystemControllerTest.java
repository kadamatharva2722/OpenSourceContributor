package com.osc.backend.system.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class SystemControllerTest {

    private final SystemController systemController = new SystemController();

    @Test
    @DisplayName("GET /api/v1/system/wake returns 204 No Content without payload")
    void wake_ReturnsNoContent() {
        ResponseEntity<Void> response = systemController.wake();

        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }
}
