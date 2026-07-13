package com.mangastudio.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "[User]")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(unique = true, length = 150)
    private String email;

    @Column(name = "phone_number", unique = true, length = 20)
    private String phoneNumber;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "profile_data", columnDefinition = "TEXT")
    private String profileData;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /** Only the most recently issued login session remains valid. */
    @JsonIgnore
    @Column(name = "active_session_id", length = 64)
    private String activeSessionId;

    public User() {}

    public User(Long id, Role role, String username, String passwordHash, String email,
                String phoneNumber, String fullName, String profileData,
                LocalDateTime createdAt, Boolean isActive, String activeSessionId) {
        this.id = id;
        this.role = role;
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.fullName = fullName;
        this.profileData = profileData;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.isActive = isActive == null ? Boolean.TRUE : isActive;
        this.activeSessionId = activeSessionId;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private Long id;
        private Role role;
        private String username;
        private String passwordHash;
        private String email;
        private String phoneNumber;
        private String fullName;
        private String profileData;
        private LocalDateTime createdAt;
        private Boolean isActive;
        private String activeSessionId;

        public Builder id(Long value) { this.id = value; return this; }
        public Builder role(Role value) { this.role = value; return this; }
        public Builder username(String value) { this.username = value; return this; }
        public Builder passwordHash(String value) { this.passwordHash = value; return this; }
        public Builder email(String value) { this.email = value; return this; }
        public Builder phoneNumber(String value) { this.phoneNumber = value; return this; }
        public Builder fullName(String value) { this.fullName = value; return this; }
        public Builder profileData(String value) { this.profileData = value; return this; }
        public Builder createdAt(LocalDateTime value) { this.createdAt = value; return this; }
        public Builder isActive(Boolean value) { this.isActive = value; return this; }
        public Builder activeSessionId(String value) { this.activeSessionId = value; return this; }

        public User build() {
            return new User(id, role, username, passwordHash, email, phoneNumber,
                    fullName, profileData, createdAt, isActive, activeSessionId);
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getProfileData() { return profileData; }
    public void setProfileData(String profileData) { this.profileData = profileData; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public String getActiveSessionId() { return activeSessionId; }
    public void setActiveSessionId(String activeSessionId) { this.activeSessionId = activeSessionId; }
}
