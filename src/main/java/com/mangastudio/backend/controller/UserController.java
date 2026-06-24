package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.request.UserProfileUpdateRequest;
import com.mangastudio.backend.dto.response.UserProfileResponse;
import com.mangastudio.backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Lấy thông tin của chính người đang đăng nhập
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getMyProfile(Authentication authentication) {
        String currentUsername = authentication.getName();
        UserProfileResponse response = userService.getUserProfile(currentUsername);
        return ResponseEntity.ok(response);
    }

    // Cập nhật thông tin của chính người đang đăng nhập
    @PutMapping("/profile")
    public ResponseEntity<UserProfileResponse> updateMyProfile(
            Authentication authentication,
            @RequestBody UserProfileUpdateRequest updateRequest) {
        
        String currentUsername = authentication.getName();
        UserProfileResponse response = userService.updateUserProfile(currentUsername, updateRequest);
        return ResponseEntity.ok(response);
    }
}