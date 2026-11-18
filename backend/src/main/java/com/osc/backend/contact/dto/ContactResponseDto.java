package com.osc.backend.contact.dto;

import com.osc.backend.contact.entity.ContactMessage;

import java.time.OffsetDateTime;

public class ContactResponseDto {

    private Long id;
    private String referenceCode;
    private String name;
    private String email;
    private String topic;
    private String subject;
    private String message;
    private String status;
    private OffsetDateTime createdAt;

    public ContactResponseDto() {
    }

    public ContactResponseDto(Long id, String referenceCode, String name, String email, String topic,
                              String subject, String message, String status, OffsetDateTime createdAt) {
        this.id = id;
        this.referenceCode = referenceCode;
        this.name = name;
        this.email = email;
        this.topic = topic;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static ContactResponseDto fromEntity(ContactMessage entity) {
        return new ContactResponseDto(
                entity.getId(),
                entity.getReferenceCode(),
                entity.getName(),
                entity.getEmail(),
                entity.getTopic(),
                entity.getSubject(),
                entity.getMessage(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReferenceCode() {
        return referenceCode;
    }

    public void setReferenceCode(String referenceCode) {
        this.referenceCode = referenceCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
