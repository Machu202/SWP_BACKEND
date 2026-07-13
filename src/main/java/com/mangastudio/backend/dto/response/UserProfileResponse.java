package com.mangastudio.backend.dto.response;

import java.time.LocalDateTime;

/**
 * Safe profile DTO used by both the personal profile screen and Admin user list.
 * It intentionally never includes passwordHash.
 */
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String phoneNumber;
    private String fullName;
    private String roleName;
    private String profileData;
    private Boolean isActive;
    private LocalDateTime createdAt;

    public UserProfileResponse() {
    }

    private UserProfileResponse(Builder builder) {
        this.id = builder.id;
        this.username = builder.username;
        this.email = builder.email;
        this.phoneNumber = builder.phoneNumber;
        this.fullName = builder.fullName;
        this.roleName = builder.roleName;
        this.profileData = builder.profileData;
        this.isActive = builder.isActive;
        this.createdAt = builder.createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }
    public String getProfileData() { return profileData; }
    public void setProfileData(String profileData) { this.profileData = profileData; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static final class Builder {
        private Long id;
        private String username;
        private String email;
        private String phoneNumber;
        private String fullName;
        private String roleName;
        private String profileData;
        private Boolean isActive;
        private LocalDateTime createdAt;

        private Builder() {
        }

        public Builder id(Long id) { this.id = id; return this; }
        public Builder username(String username) { this.username = username; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder roleName(String roleName) { this.roleName = roleName; return this; }
        public Builder profileData(String profileData) { this.profileData = profileData; return this; }
        public Builder isActive(Boolean isActive) { this.isActive = isActive; return this; }
        public Builder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserProfileResponse build() { return new UserProfileResponse(this); }
    }
}
