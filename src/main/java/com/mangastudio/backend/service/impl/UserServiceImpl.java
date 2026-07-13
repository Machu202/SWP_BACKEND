package com.mangastudio.backend.service.impl;

import com.mangastudio.backend.dto.request.UserProfileUpdateRequest;
import com.mangastudio.backend.dto.response.UserProfileResponse;
import com.mangastudio.backend.entity.Role;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

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

        if (request.getFullName() != null) {
            user.setFullName(normalizeNullable(request.getFullName()));
        }
        if (request.getPhoneNumber() != null) {
            String normalizedPhone = normalizeNullable(request.getPhoneNumber());
            if (normalizedPhone != null
                    && !normalizedPhone.equals(user.getPhoneNumber())
                    && userRepository.existsByPhoneNumber(normalizedPhone)) {
                throw new RuntimeException("Phone number is already used by another account.");
            }
            user.setPhoneNumber(normalizedPhone);
        }
        if (request.getProfileData() != null) {
            user.setProfileData(normalizeNullable(request.getProfileData()));
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

    @Override
    public List<UserProfileResponse> getUsersByRole(String roleName) {
        return userRepository.findByRole_RoleName(roleName).stream()
                .map(this::mapToProfileResponse) // Tận dụng lại mapper DTO sẵn có giúp bảo mật giấu passwordHash
                .collect(Collectors.toList());
    }
    
    private UserProfileResponse mapToProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .fullName(user.getFullName())
                .roleName(user.getRole().getRoleName())
                .profileData(user.getProfileData())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String normalizeNullable(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
