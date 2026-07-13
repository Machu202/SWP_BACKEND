package com.mangastudio.backend.dto.request;

/** Fields the authenticated user may update on their own profile. */
public class UserProfileUpdateRequest {
    private String fullName;
    private String phoneNumber;
    private String profileData;

    public UserProfileUpdateRequest() {
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getProfileData() { return profileData; }
    public void setProfileData(String profileData) { this.profileData = profileData; }
}
