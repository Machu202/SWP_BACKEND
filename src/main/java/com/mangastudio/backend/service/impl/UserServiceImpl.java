package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.request.UserProfileUpdateRequest;
import com.mangastudio.backend.dto.response.UserProfileResponse;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserProfileResponse getUserProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Error: User not found with username: " + username));

        return mapToProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateUserProfile(String username, UserProfileUpdateRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Error: User not found with username: " + username));

        // Update fields if they are provided in the request
        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getProfileData() != null && !request.getProfileData().isBlank()) {
            user.setProfileData(request.getProfileData());
        }

        User updatedUser = userRepository.save(user);
        
        return mapToProfileResponse(updatedUser);
    }

    // Helper method to map Entity to DTO
    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleName(user.getRole().getRoleName())
                .profileData(user.getProfileData())
                .build();
    }
}