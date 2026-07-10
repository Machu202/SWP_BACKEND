package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.request.GoogleLoginRequest;
import com.mangastudio.backend.dto.request.LoginRequest;
import com.mangastudio.backend.dto.request.RegisterRequest;
import com.mangastudio.backend.dto.request.VerifyOtpRequest;
import com.mangastudio.backend.dto.response.JwtResponse;
import com.mangastudio.backend.dto.response.MessageResponse;
import com.mangastudio.backend.security.JwtUtils;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;

    // ========================================================
    // ĐÃ SỬA: ĐĂNG NHẬP TRẢ VỀ TOKEN TRỰC TIẾP, BỎ QUA OTP
    // ========================================================
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        
        // 1. Kiểm tra tài khoản và mật khẩu
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        // 2. Nếu mật khẩu ĐÚNG, lưu trạng thái vào Security Context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Tạo chìa khóa JWT Token ngay lập tức
        String jwt = jwtUtils.generateJwtToken(authentication);

        // 4. Lấy thông tin chi tiết của User
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        
        // 5. Lấy Quyền (Role) của User để Frontend biết đường chuyển trang
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(item -> item.getAuthority())
                .orElse("Mangaka"); // Mặc định nếu lỗi phân quyền

        // 6. Đóng gói dữ liệu trả thẳng về cho Frontend
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", jwt);
        responseBody.put("type", "Bearer");
        responseBody.put("id", userDetails.getId());
        responseBody.put("role", role);
        responseBody.put("username", userDetails.getUsername());
        responseBody.put("email", userDetails.getEmail());
        responseBody.put("message", "Đăng nhập thành công!");

        // Trả về HTTP 200 OK kèm theo Token
        return ResponseEntity.ok(responseBody);
    }

    // ========================================================
    // CÁC HÀM CŨ ĐƯỢC GIỮ NGUYÊN ĐỂ KHÔNG BỊ LỖI ĐỎ
    // ========================================================

    // Request OTP after checking email/username + password.
    // Frontend calls this first, then calls /verify-otp with the email + OTP code.
    @PostMapping("/request-otp")
    public ResponseEntity<MessageResponse> requestOtp(@Valid @RequestBody LoginRequest loginRequest) {
        MessageResponse response = authService.authenticateUserAndGenerateOtp(loginRequest);
        return ResponseEntity.ok(response);
    }

    // Nhập OTP -> Trả về JWT Token (Giữ lại làm phương án dự phòng)
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

    // Đăng nhập trực tiếp bằng Google OAuth2
    @PostMapping("/google")
    public ResponseEntity<JwtResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        JwtResponse response = authService.googleLogin(request);
        return ResponseEntity.ok(response);
    }
}