package com.osc.backend.userpreference.entity;

import com.osc.backend.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity representing user discovery preferences, mapped to "user_preferences".
 */
@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_languages", columnDefinition = "jsonb", nullable = false)
    private List<String> preferredLanguages = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "preferred_domains", columnDefinition = "jsonb", nullable = false)
    private List<String> preferredDomains = new ArrayList<>();

    @Column(name = "difficulty", length = 50)
    private String difficulty;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UserPreference() {
    }

    public UserPreference(User user, List<String> preferredLanguages, List<String> preferredDomains, String difficulty) {
        this.user = user;
        this.preferredLanguages = preferredLanguages != null ? preferredLanguages : new ArrayList<>();
        this.preferredDomains = preferredDomains != null ? preferredDomains : new ArrayList<>();
        this.difficulty = difficulty;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<String> getPreferredLanguages() {
        if (preferredLanguages == null) {
            preferredLanguages = new ArrayList<>();
        }
        return preferredLanguages;
    }

    public void setPreferredLanguages(List<String> preferredLanguages) {
        this.preferredLanguages = preferredLanguages != null ? preferredLanguages : new ArrayList<>();
    }

    public List<String> getPreferredDomains() {
        if (preferredDomains == null) {
            preferredDomains = new ArrayList<>();
        }
        return preferredDomains;
    }

    public void setPreferredDomains(List<String> preferredDomains) {
        this.preferredDomains = preferredDomains != null ? preferredDomains : new ArrayList<>();
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "UserPreference{" +
                "id=" + id +
                ", userId=" + (user != null ? user.getId() : null) +
                ", preferredLanguages=" + preferredLanguages +
                ", preferredDomains=" + preferredDomains +
                ", difficulty='" + difficulty + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
