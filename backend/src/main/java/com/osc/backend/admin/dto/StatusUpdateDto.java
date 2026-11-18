package com.osc.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StatusUpdateDto {

    @NotBlank(message = "Status cannot be blank")
    @Size(max = 30, message = "Status must not exceed 30 characters")
    private String status;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;

    public StatusUpdateDto() {
    }

    public StatusUpdateDto(String status, String notes) {
        this.status = status;
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
