package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.request.UserProfileUpdateRequest;
import com.mangastudio.backend.dto.response.UserProfileResponse;

public interface UserService {
    UserProfileResponse getUserProfile(String username);
    UserProfileResponse updateUserProfile(String username, UserProfileUpdateRequest updateRequest);
}