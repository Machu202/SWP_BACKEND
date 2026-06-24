package com.mangastudio.backend.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileUpdateRequest {
    
    private String fullName;
    
    // JSON string containing dynamic profile information (e.g., avatar URL, bio, portfolio link)
    private String profileData; 
}