package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.request.UserProfileUpdateRequest;
import com.mangastudio.backend.dto.response.UserProfileResponse;
import com.mangastudio.backend.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
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

    // --- ENDPOINTS FOR AUTHENTICATED USERS ---

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

    // --- ADMIN-ONLY ENDPOINTS (FE-03) ---

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileResponse>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // Locks or unlocks an account with isActive=true/false.
    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> toggleUserLock(
            @PathVariable Long id,
            @RequestParam boolean isActive) {
        UserProfileResponse response = userService.toggleUserLock(id, isActive);
        return ResponseEntity.ok(response);
    }

    // Assigns a role with roleName="Mangaka", "Assistant", "Admin", and so on.
    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileResponse> assignRole(
            @PathVariable Long id,
            @RequestParam String roleName) {
        UserProfileResponse response = userService.assignRole(id, roleName);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "Filter users by role (VD: ?role=Assistant)")
    public ResponseEntity<List<UserProfileResponse>> getUsersByRole(@RequestParam String role) {
        List<UserProfileResponse> users = userService.getUsersByRole(role);
        return ResponseEntity.ok(users);
    }
}
