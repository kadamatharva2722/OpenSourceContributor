package com.osc.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for administrator password verification (second-factor authentication).
 */
public class AdminPasswordVerifyRequestDto {

    @NotBlank(message = "Administrator password is required")
    private String password;

    public AdminPasswordVerifyRequestDto() {}

    public AdminPasswordVerifyRequestDto(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Prevents accidental leakage of the administrator password in log statements.
     */
    @Override
    public String toString() {
        return "AdminPasswordVerifyRequestDto{password='[PROTECTED]'}";
    }
}
