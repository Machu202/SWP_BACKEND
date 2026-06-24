package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.request.UserProfileUpdateRequest;
import com.mangastudio.backend.dto.response.UserProfileResponse;
import com.mangastudio.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // --- CÁC API DÀNH CHO USER ĐANG ĐĂNG NHẬP ---

    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        String currentUsername = authentication.getName();
        UserProfileResponse response = userService.getUserProfile(currentUsername);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            Authentication authentication,
            @RequestBody UserProfileUpdateRequest updateRequest) {
        
        String currentUsername = authentication.getName();
        UserProfileResponse response = userService.updateUserProfile(currentUsername, updateRequest);
        return ResponseEntity.ok(response);
    }

    // --- CÁC API ĐẶC QUYỀN DÀNH CHO ADMIN (FE-03) ---

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // API Khóa hoặc Mở khóa tài khoản (Truyền tham số isActive=true/false)
    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> toggleUserLock(
            @PathVariable Long id,
            @RequestParam boolean isActive) {
        UserProfileResponse response = userService.toggleUserLock(id, isActive);
        return ResponseEntity.ok(response);
    }

    // API Gán quyền mới cho User (Truyền tham số roleName="Mangaka", "Assistant", "Admin"...)
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> assignRole(
            @PathVariable Long id,
            @RequestParam String roleName) {
        UserProfileResponse response = userService.assignRole(id, roleName);
        return ResponseEntity.ok(response);
    }
}