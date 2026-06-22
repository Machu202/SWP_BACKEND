package com.mangastudio.backend.controller;

import com.mangastudio.backend.dto.AuthResponse;
import com.mangastudio.backend.dto.LoginRequest;
import com.mangastudio.backend.dto.OtpVerificationRequest;
import com.mangastudio.backend.entity.User;
import com.mangastudio.backend.repository.RoleRepository;
import com.mangastudio.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"}) // Resolve CORS policy for Frontend integration
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JavaMailSender mailSender;

    // ==========================================
    // 1. REGISTRATION FLOW
    // ==========================================
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody LoginRequest request) {
        
        // Step 1: Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body("Username already exists!");
        }

        // Step 2: Formulate email and check for duplication
        // Note: Using username as email prefix for simulation. In production, use a dedicated RegisterRequest DTO.
        String newEmail = request.getUsername() + "@gmail.com"; 
        
        if (userRepository.existsByEmail(newEmail)) {
            return ResponseEntity.badRequest().body("Email is already in use by another user!");
        }

        try {
            // Step 3: Send verification email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(newEmail);
            message.setSubject("Welcome to Manga Studio");
            message.setText("Hello " + request.getUsername() + ",\n\nYour account has been successfully created on our platform!");
            
            mailSender.send(message);

            // Step 4: Persist new user to the database only after email is successfully sent
            User newUser = new User();
            newUser.setUsername(request.getUsername());
            newUser.setPassword(request.getPassword());
            newUser.setEmail(newEmail);
            newUser.setRole(roleRepository.findByRoleName("READER")); // Assign default role
            
            userRepository.save(newUser);

            return ResponseEntity.ok("Registration successful! Please check your email.");

        } catch (Exception e) {
            // Revert process if email fails to send (e.g., invalid email or SMTP issues)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to send verification email. Please ensure the email address is valid!");
        }
    }

    // ==========================================
    // 2. LOGIN FLOW (STEP 1) - VERIFY CREDENTIALS & SEND OTP
    // ==========================================
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername());

        // Step 1: Verify Username and Password
        if (user != null && user.getPassword().equals(request.getPassword())) {
            
            // Step 2: Generate a 6-digit OTP
            String generatedOtp = String.format("%06d", new Random().nextInt(999999));
            
            // Step 3: Save OTP to Database with a 5-minute expiration window
            user.setOtpCode(generatedOtp);
            user.setOtpExpiration(LocalDateTime.now().plusMinutes(5));
            userRepository.save(user);

            // Step 4: Send OTP via Email
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(user.getEmail());
                message.setSubject("Manga Studio - Your Login OTP");
                message.setText("Your OTP for login is: " + generatedOtp + 
                                "\nThis code will expire in 5 minutes. Do not share it with anyone.");
                
                mailSender.send(message);
                
                return ResponseEntity.ok("Credentials verified. Please check your email for the OTP.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Failed to send OTP email. Please try again.");
            }
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password!");
    }

    // ==========================================
    // 3. LOGIN FLOW (STEP 2) - VERIFY OTP & ISSUE TOKEN
    // ==========================================
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody OtpVerificationRequest request) {
        User user = userRepository.findByUsername(request.getUsername());

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found!");
        }

        // Step 1: Validate OTP matching
        if (request.getOtpCode() != null && request.getOtpCode().equals(user.getOtpCode())) {
            
            // Step 2: Check for OTP expiration
            if (LocalDateTime.now().isAfter(user.getOtpExpiration())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("OTP has expired!");
            }

            // Step 3: Nullify the OTP to prevent reuse vulnerability
            user.setOtpCode(null);
            user.setOtpExpiration(null);
            userRepository.save(user);

            // Step 4: Generate and return the Authentication Token (Mocked JWT)
            String mockToken = "MS-JWT-" + UUID.randomUUID().toString();
            AuthResponse response = new AuthResponse(
                mockToken,
                user.getUsername(),
                user.getRole().getRoleName(),
                user.getEmail()
            );
            
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid OTP!");
    }
}