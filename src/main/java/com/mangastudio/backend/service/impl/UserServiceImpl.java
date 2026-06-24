package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.request.UserProfileUpdateRequest;
import com.mangastudio.backend.dto.response.UserProfileResponse;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository; // Tiêm kho chứa Role vào đây

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

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getProfileData() != null && !request.getProfileData().isBlank()) {
            user.setProfileData(request.getProfileData());
        }

        User updatedUser = userRepository.save(user);
        return mapToProfileResponse(updatedUser);
    }

    // [BỔ SUNG FE-03] Lấy tất cả user
    @Override
    public List<UserProfileResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToProfileResponse)
                .collect(Collectors.toList());
    }

    // [BỔ SUNG FE-03] Khóa/Mở khóa tài khoản
    @Override
    @Transactional
    public UserProfileResponse toggleUserLock(Long userId, boolean isActive) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found with ID: " + userId));
        
        user.setIsActive(isActive);
        User updatedUser = userRepository.save(user);
        return mapToProfileResponse(updatedUser);
    }

    // [BỔ SUNG FE-03] Phân quyền tài khoản
    @Override
    @Transactional
    public UserProfileResponse assignRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Error: User not found with ID: " + userId));
                
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Error: Role not found: " + roleName));
                
        user.setRole(role);
        User updatedUser = userRepository.save(user);
        return mapToProfileResponse(updatedUser);
    }

    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .roleName(user.getRole().getRoleName())
                .profileData(user.getProfileData())
                .isActive(user.getIsActive()) // Cập nhật mapping
                .build();
    }
}