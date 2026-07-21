package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.request.GoogleLoginRequest;
import com.mangastudio.backend.dto.request.LoginRequest;
import com.mangastudio.backend.dto.request.RegisterRequest;
import com.mangastudio.backend.dto.request.RequestOtpRequest;
import com.mangastudio.backend.dto.request.VerifyOtpRequest;
import com.mangastudio.backend.dto.response.JwtResponse;
import com.mangastudio.backend.dto.response.MessageResponse;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.UserRepository;
import com.mangastudio.backend.security.JwtUtils;
import com.mangastudio.backend.security.UserDetailsImpl;
import com.mangastudio.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          JwtUtils jwtUtils, UserRepository userRepository) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        User user = userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new RuntimeException("Error: User not found"));
        String sessionId = UUID.randomUUID().toString();
        user.setActiveSessionId(sessionId);
        userRepository.saveAndFlush(user);

        String jwt = jwtUtils.generateJwtToken(authentication, sessionId);
        String role = userDetails.getAuthorities().stream()
                .findFirst()
                .map(item -> item.getAuthority())
                .orElse("ROLE_MANGAKA");

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("token", jwt);
        responseBody.put("type", "Bearer");
        responseBody.put("id", userDetails.getId());
        responseBody.put("role", role);
        responseBody.put("username", userDetails.getUsername());
        responseBody.put("email", userDetails.getEmail());
        responseBody.put("message", "Login successful!");
        return ResponseEntity.ok(responseBody);
    }

    /** Heartbeat endpoint used by the frontend to detect a newer login. */
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> session(Authentication authentication) {
        UserDetailsImpl user = (UserDetailsImpl) authentication.getPrincipal();
        Map<String, Object> response = new HashMap<>();
        response.put("active", true);
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            Authentication authentication,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl details) {
            User user = userRepository.findById(details.getId()).orElse(null);
            String token = bearerToken(authorization);
            String sessionId = token.isBlank() ? "" : jwtUtils.getSessionIdFromJwtToken(token);
            if (user != null && sessionId.equals(user.getActiveSessionId())) {
                user.setActiveSessionId(null);
                userRepository.save(user);
            }
        }
        return ResponseEntity.ok(new MessageResponse("Logged out."));
    }

    @PostMapping("/request-otp")
    public ResponseEntity<MessageResponse> requestOtp(@Valid @RequestBody RequestOtpRequest request) {
        return ResponseEntity.ok(authService.generateOtpForEmail(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<JwtResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtpAndLogin(request));
    }

    @PostMapping("/register")
    public ResponseEntity<MessageResponse> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        return ResponseEntity.ok(authService.registerUser(registerRequest));
    }

    @PostMapping("/google")
    public ResponseEntity<JwtResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        return ResponseEntity.ok(authService.googleLogin(request));
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return "";
        return authorization.substring(7);
    }
}
