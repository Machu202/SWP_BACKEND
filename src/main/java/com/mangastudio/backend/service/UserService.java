package com.mangastudio.backend.service;

import com.mangastudio.backend.dto.request.UserProfileUpdateRequest;
import com.mangastudio.backend.dto.response.UserProfileResponse;
import java.util.List;

public interface UserService {
    UserProfileResponse getUserProfile(String username);
    UserProfileResponse updateUserProfile(String username, UserProfileUpdateRequest updateRequest);
    
    // [FE-03] Admin-only operations.
    List<UserProfileResponse> getAllUsers();
    UserProfileResponse toggleUserLock(Long userId, boolean isActive);
    UserProfileResponse assignRole(Long userId, String roleName, Long currentAdminId);
    
    List<UserProfileResponse> getUsersByRole(String roleName);
}
