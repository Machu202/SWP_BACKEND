package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.request.GoogleLoginRequest;
import com.mangastudio.backend.dto.request.LoginRequest;
import com.mangastudio.backend.dto.request.RegisterRequest;
import com.mangastudio.backend.dto.request.VerifyOtpRequest;
import com.mangastudio.backend.dto.response.JwtResponse;
import com.mangastudio.backend.dto.response.MessageResponse;
import com.mangastudio.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // Bước 1: Đăng nhập -> Trả về thông báo yêu cầu check Email (Không trả JWT nữa)
    @PostMapping("/login")
    public ResponseEntity<MessageResponse> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        MessageResponse response = authService.authenticateUserAndGenerateOtp(loginRequest);
        return ResponseEntity.ok(response);
    }

    // Bước 2: Nhập OTP -> Trả về JWT Token
    @PostMapping("/verify-otp")
    public ResponseEntity<JwtResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        JwtResponse response = authService.verifyOtpAndLogin(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        MessageResponse response = authService.registerUser(registerRequest);
        return ResponseEntity.ok(response);
    }

    // Bước 3 của Auth Flow: Đăng nhập trực tiếp bằng Google OAuth2
    @PostMapping("/google")
    public ResponseEntity<JwtResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        JwtResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(response);
    }
}